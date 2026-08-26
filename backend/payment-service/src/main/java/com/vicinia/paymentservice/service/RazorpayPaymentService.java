package com.vicinia.paymentservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vicinia.paymentservice.client.RazorpayClient;
import com.vicinia.paymentservice.domain.RazorpayPayment;
import com.vicinia.paymentservice.domain.RazorpayPaymentStatus;
import com.vicinia.paymentservice.dto.RazorpayOrderResponse;
import com.vicinia.paymentservice.exception.ForbiddenException;
import com.vicinia.paymentservice.exception.RazorpayPaymentNotFoundException;
import com.vicinia.paymentservice.exception.RazorpaySignatureInvalidException;
import com.vicinia.paymentservice.messaging.PaymentEventPublisher;
import com.vicinia.paymentservice.repository.RazorpayPaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class RazorpayPaymentService {

    private static final String CURRENCY = "INR";

    private final RazorpayPaymentRepository repository;
    private final RazorpayClient razorpayClient;
    private final PaymentEventPublisher eventPublisher;
    private final String webhookSecret;
    private final String keySecret;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RazorpayPaymentService(RazorpayPaymentRepository repository,
                                   RazorpayClient razorpayClient,
                                   PaymentEventPublisher eventPublisher,
                                   @Value("${vicinia.razorpay.webhook-secret}") String webhookSecret,
                                   @Value("${vicinia.razorpay.key-secret}") String keySecret) {
        this.repository = repository;
        this.razorpayClient = razorpayClient;
        this.eventPublisher = eventPublisher;
        this.webhookSecret = webhookSecret;
        this.keySecret = keySecret;
    }

    /** Idempotent on orderId — a retried create-order call returns the same Razorpay order rather than creating a second one Razorpay-side. */
    @Transactional
    public RazorpayOrderResponse createOrder(UUID userId, UUID orderId, BigDecimal amount) {
        Optional<RazorpayPayment> existing = repository.findByOrderId(orderId);
        if (existing.isPresent()) {
            RazorpayPayment payment = existing.get();
            return new RazorpayOrderResponse(orderId, payment.getRazorpayOrderId(), razorpayClient.getKeyId(), payment.getAmount(), CURRENCY);
        }

        String razorpayOrderId = razorpayClient.createOrder(amount, orderId.toString());
        repository.save(new RazorpayPayment(orderId, userId, razorpayOrderId, amount));
        return new RazorpayOrderResponse(orderId, razorpayOrderId, razorpayClient.getKeyId(), amount, CURRENCY);
    }

    /**
     * ARCHITECTURE.md §4.6: verify signature, then resolve idempotently.
     * tryResolve's atomic conditional UPDATE (rows-affected 0 on a replay)
     * is what actually prevents double-processing — the DB-level unique
     * constraint on razorpay_payment_id is a backstop against a different
     * failure mode (the same payment id ever attaching to two different
     * orders), not the primary idempotency mechanism.
     */
    @Transactional
    public void handleWebhook(String rawBody, String signature) {
        if (!isValidSignature(rawBody, signature)) {
            throw new RazorpaySignatureInvalidException();
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(rawBody);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Malformed webhook payload", e);
        }

        String eventType = root.path("event").asText();
        JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
        String razorpayOrderId = paymentEntity.path("order_id").asText();
        String razorpayPaymentId = paymentEntity.path("id").asText();

        RazorpayPayment payment = repository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new RazorpayPaymentNotFoundException(razorpayOrderId));

        RazorpayPaymentStatus targetStatus = "payment.captured".equals(eventType)
                ? RazorpayPaymentStatus.SUCCESS
                : RazorpayPaymentStatus.FAILED;

        resolve(payment, targetStatus, razorpayPaymentId);
    }

    /**
     * The client-side counterpart to handleWebhook, for environments where
     * Razorpay's real servers can't reach this service's webhook at all —
     * a local dev stack has no public URL, so the webhook this project
     * otherwise relies on (ARCHITECTURE.md §4.6) never arrives and an
     * order sits at PAYMENT_PENDING forever even though checkout.js's own
     * modal already reported success. This verifies the same way Razorpay
     * documents for client-side confirmation — HMAC-SHA256 over
     * "{order_id}|{payment_id}" keyed with the account's key SECRET (not
     * the separate webhook secret used above) — so it's exactly as trusted
     * as the webhook path, just triggered by the browser's own callback
     * instead of an inbound call. tryResolve's atomic guard means it's
     * harmless if a real webhook also arrives later and races this.
     */
    @Transactional
    public void verifyAndResolve(UUID userId, String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        RazorpayPayment payment = repository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new RazorpayPaymentNotFoundException(razorpayOrderId));
        if (!payment.getUserId().equals(userId)) {
            throw new ForbiddenException("This payment does not belong to you");
        }

        String toSign = razorpayOrderId + "|" + razorpayPaymentId;
        if (!isValidSignature(toSign, razorpaySignature, keySecret)) {
            throw new RazorpaySignatureInvalidException();
        }

        resolve(payment, RazorpayPaymentStatus.SUCCESS, razorpayPaymentId);
    }

    private void resolve(RazorpayPayment payment, RazorpayPaymentStatus targetStatus, String razorpayPaymentId) {
        int updated = repository.tryResolve(payment.getId(), targetStatus, razorpayPaymentId);
        if (updated == 0) {
            return;
        }

        if (targetStatus == RazorpayPaymentStatus.SUCCESS) {
            eventPublisher.publishSuccess(payment.getOrderId(), payment.getUserId(), payment.getAmount(), "RAZORPAY");
        } else {
            eventPublisher.publishFailed(payment.getOrderId(), payment.getUserId(), payment.getAmount(), "RAZORPAY");
        }
    }

    /** The webhook's own signature, over the exact raw request body bytes — Razorpay computes it over the literal bytes it sent, and re-serializing (even a semantically identical JSON tree) can silently change whitespace or key order and break verification. */
    private boolean isValidSignature(String rawBody, String signature) {
        return isValidSignature(rawBody, signature, webhookSecret);
    }

    /**
     * Shared HMAC-SHA256 check for both the webhook (message = raw body,
     * secret = webhook secret) and client-side verify (message =
     * "{order_id}|{payment_id}", secret = the account's key secret) —
     * Razorpay uses the same algorithm for both, just different messages
     * and keys. MessageDigest.isEqual is a constant-time comparison — this
     * is exactly the kind of check where a timing side-channel is a real,
     * documented concern.
     */
    private boolean isValidSignature(String message, String signature, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(hash);
            return MessageDigest.isEqual(
                    computed.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | java.security.InvalidKeyException e) {
            throw new IllegalStateException("Failed to compute signature", e);
        }
    }
}
