package com.vicinia.orderservice.client;

import com.vicinia.orderservice.exception.PaymentGatewayUnavailableException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class PaymentClient {

    private final RestTemplate restTemplate;

    public PaymentClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Returns false on 402 (insufficient balance) — an expected, handled
     * outcome. Anything else propagates as a real error.
     *
     * <p>@Retryable (Stage 15, ARCHITECTURE.md §12) on transient failures
     * only — never on the 402 handled above. Safe to retry because wallet
     * pay is idempotent on orderId per §11 (a unique constraint on one
     * successful transaction per order) — a retry after a response that
     * actually succeeded server-side is a no-op, not a double-charge.
     */
    @Retryable(retryFor = {ResourceAccessException.class, HttpServerErrorException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2.0))
    public boolean payWithWallet(UUID userId, UUID orderId, BigDecimal amount) {
        try {
            restTemplate.postForEntity(
                    "http://PAYMENT-SERVICE/api/payments/wallet/pay", new PayRequest(userId, orderId, amount), Void.class);
            return true;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.PAYMENT_REQUIRED) {
                return false;
            }
            throw e;
        }
    }

    public void refund(UUID userId, UUID orderId, BigDecimal amount) {
        restTemplate.postForEntity(
                "http://PAYMENT-SERVICE/api/payments/wallet/refund", new RefundRequest(userId, orderId, amount), Void.class);
    }

    /**
     * Genuinely async from here — no success/failure to branch on
     * synchronously, the order stays PAYMENT_PENDING until the webhook
     * resolves it (see PaymentEventConsumer).
     *
     * <p>@CircuitBreaker + @Bulkhead (Stage 15, ARCHITECTURE.md §12), not
     * @Retryable: creating a Razorpay order isn't idempotent to blindly
     * retry (a naive retry would mint a second, distinct Razorpay order),
     * and Razorpay's own degradation is exactly the "third-party
     * dependency shouldn't take down order creation for wallet-only
     * customers too" case the circuit breaker exists for. The bulkhead is
     * a semaphore, not a thread pool — it doesn't move this call onto a
     * separate executor, but it caps how many of the caller's own request
     * threads can be blocked inside it at once, which is what actually
     * protects the pool wallet-only checkout depends on from being
     * starved by a slow/hanging Razorpay. Both are scoped to the
     * "razorpay" instance only (application.yml) — payWithWallet above has
     * neither, deliberately, since it has no dependency on Razorpay at all.
     */
    @CircuitBreaker(name = "razorpay", fallbackMethod = "razorpayUnavailable")
    @Bulkhead(name = "razorpay")
    public RazorpayOrderResponse createRazorpayOrder(UUID userId, UUID orderId, BigDecimal amount) {
        return restTemplate.postForObject(
                "http://PAYMENT-SERVICE/api/payments/razorpay/order",
                new RazorpayOrderRequest(userId, orderId, amount), RazorpayOrderResponse.class);
    }

    @SuppressWarnings("unused")
    private RazorpayOrderResponse razorpayUnavailable(UUID userId, UUID orderId, BigDecimal amount, Throwable t) {
        throw new PaymentGatewayUnavailableException(t);
    }

    public record PayRequest(UUID userId, UUID orderId, BigDecimal amount) {
    }

    public record RefundRequest(UUID userId, UUID orderId, BigDecimal amount) {
    }

    public record RazorpayOrderRequest(UUID userId, UUID orderId, BigDecimal amount) {
    }

    public record RazorpayOrderResponse(UUID orderId, String razorpayOrderId, String razorpayKeyId, BigDecimal amount, String currency) {
    }
}
