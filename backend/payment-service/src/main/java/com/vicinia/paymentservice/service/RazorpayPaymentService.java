package com.vicinia.paymentservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vicinia.paymentservice.client.RazorpayClient;
import com.vicinia.paymentservice.domain.RazorpayPayment;
import com.vicinia.paymentservice.domain.RazorpayPaymentStatus;
import com.vicinia.paymentservice.dto.RazorpayOrderResponse;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RazorpayPaymentService(RazorpayPaymentRepository repository,
                                   RazorpayClient razorpayClient,
                                   PaymentEventPublisher eventPublisher,
                                   @Value("${vicinia.razorpay.webhook-secret}") String webhookSecret) {
        this.repository = repository;
        this.razorpayClient = razorpayClient;
        this.eventPublisher = eventPublisher;
        this.webhookSecret = webhookSecret;
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

    /**
     * Verifies against the exact raw request body bytes, not a
     * re-serialized version of a parsed object — Razorpay computes its
     * signature over the literal bytes it sent, and re-serializing (even a
     * semantically identical JSON tree) can silently change whitespace or
     * key order and break verification. MessageDigest.isEqual is a
     * constant-time comparison — a webhook signature is exactly the kind
     * of check where a timing side-channel is a real, documented concern.
     */
    private boolean isValidSignature(String rawBody, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(hash);
            return MessageDigest.isEqual(
                    computed.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | java.security.InvalidKeyException e) {
            throw new IllegalStateException("Failed to compute webhook signature", e);
        }
    }
}
