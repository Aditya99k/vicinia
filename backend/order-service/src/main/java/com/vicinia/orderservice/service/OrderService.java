package com.vicinia.orderservice.service;

import com.vicinia.orderservice.client.CartClient;
import com.vicinia.orderservice.client.CouponClient;
import com.vicinia.orderservice.client.InventoryClient;
import com.vicinia.orderservice.client.PaymentClient;
import com.vicinia.orderservice.domain.Order;
import com.vicinia.orderservice.domain.OrderItem;
import com.vicinia.orderservice.domain.OrderStatus;
import com.vicinia.orderservice.dto.PaymentMethod;
import com.vicinia.orderservice.dto.PlaceOrderRequest;
import com.vicinia.orderservice.dto.PlaceOrderResult;
import com.vicinia.orderservice.exception.CartHasUnavailableItemsException;
import com.vicinia.orderservice.exception.EmptyCartException;
import com.vicinia.orderservice.exception.ForbiddenException;
import com.vicinia.orderservice.exception.OrderNotFoundException;
import com.vicinia.orderservice.messaging.OrderEventPublisher;
import com.vicinia.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Deliberately has no @Transactional anywhere in this class, and every
 * write here is exactly one orderRepository.save(...) call — never more
 * than one per method. Two things make that the right call, not an
 * oversight:
 *
 * <p>First, wrapping placeOrder's whole flow in one transaction would
 * repeat exactly the class of bug fixed in Stage 5 (a Kafka publish inside
 * a transaction that later rolled back), generalized to "an external REST
 * call inside a transaction that might still roll back" — reserve()
 * succeeding against inventory-service has to be durably reflected in this
 * service's own DB immediately, not held open across the payment step that
 * follows it.
 *
 * <p>Second, adding @Transactional to the small helper methods below
 * wouldn't even work correctly if placeOrder called them the obvious way:
 * Spring's @Transactional is proxy-based, and a same-class call
 * (this.createOrder(...) from inside placeOrder) bypasses that proxy
 * entirely — a well-known Spring pitfall. Rather than route around it with
 * self-injection or a second bean, this relies on a simpler fact: Spring
 * Data's own JpaRepository methods are already individually transactional
 * by default, so a single save() call is already a complete, atomically
 * committed unit of work with nothing extra needed.
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartClient cartClient;
    private final InventoryClient inventoryClient;
    private final CouponClient couponClient;
    private final PaymentClient paymentClient;
    private final OrderEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository, CartClient cartClient, InventoryClient inventoryClient,
                         CouponClient couponClient, PaymentClient paymentClient, OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.cartClient = cartClient;
        this.inventoryClient = inventoryClient;
        this.couponClient = couponClient;
        this.paymentClient = paymentClient;
        this.eventPublisher = eventPublisher;
    }

    public PlaceOrderResult placeOrder(UUID userId, PlaceOrderRequest request) {
        CartClient.CartView cart = cartClient.getCart(userId);
        if (cart.items().isEmpty()) {
            throw new EmptyCartException();
        }
        if (cart.items().stream().anyMatch(line -> !line.available())) {
            throw new CartHasUnavailableItemsException();
        }

        Order order = createOrder(userId, cart);
        eventPublisher.publishCreated(order.getId(), userId);

        if (request.couponCode() != null && !request.couponCode().isBlank()) {
            Optional<BigDecimal> discount = couponClient.apply(userId, request.couponCode(), order.getId(), cart.subtotal());
            if (discount.isEmpty()) {
                return PlaceOrderResult.wallet(cancelOrder(order, "Coupon rejected: " + request.couponCode()));
            }
            order = applyCoupon(order, request.couponCode(), discount.get());
        }

        List<InventoryClient.ReserveItem> reserveItems = cart.items().stream()
                .map(line -> new InventoryClient.ReserveItem(line.listingId(), line.quantity()))
                .toList();

        boolean reserved = inventoryClient.reserve(order.getId(), reserveItems);
        if (!reserved) {
            return PlaceOrderResult.wallet(cancelOrder(order, "Insufficient stock"));
        }

        order = markPaymentPending(order);

        PaymentMethod method = request.paymentMethod() != null ? request.paymentMethod() : PaymentMethod.WALLET;

        if (method == PaymentMethod.RAZORPAY) {
            var razorpayOrder = paymentClient.createRazorpayOrder(userId, order.getId(), order.getTotalAmount());
            return new PlaceOrderResult(order, razorpayOrder.razorpayOrderId(), razorpayOrder.razorpayKeyId());
        }

        boolean paid = paymentClient.payWithWallet(userId, order.getId(), order.getTotalAmount());
        if (!paid) {
            inventoryClient.release(order.getId());
            return PlaceOrderResult.wallet(markPaymentFailed(order));
        }

        inventoryClient.confirm(order.getId());
        Order confirmed = markConfirmed(order);
        eventPublisher.publishConfirmed(confirmed.getId(), userId, confirmed.getMerchantId());
        return PlaceOrderResult.wallet(confirmed);
    }

    /**
     * Reached only via PaymentEventConsumer, for the Razorpay path — wallet
     * never needs this, its own synchronous flow above already confirms or
     * fails the order in the same request. Idempotent exactly per ADR
     * 0004/§4.6: only acts while the order is still PAYMENT_PENDING, so a
     * replayed webhook-driven event is a safe no-op rather than an
     * illegal-transition error.
     */
    public void confirmFromPaymentEvent(UUID orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            return;
        }
        inventoryClient.confirm(orderId);
        Order confirmed = markConfirmed(order);
        eventPublisher.publishConfirmed(confirmed.getId(), confirmed.getUserId(), confirmed.getMerchantId());
    }

    public void failFromPaymentEvent(UUID orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            return;
        }
        inventoryClient.release(orderId);
        markPaymentFailed(order);
    }

    /**
     * Reached only via OrderFulfillmentConsumer, from merchant-service's
     * merchant.accepted event (Stage 11 — pulled forward, see
     * BUILD_TRACKER.md's notes). Two transitions in one call — CONFIRMED
     * -> MERCHANT_ACCEPTED -> PREPARING — the same reasoning as Stage 3's
     * own merchant.approve(): there's no genuinely separate signal for "a
     * merchant clicked accept" versus "the merchant started preparing," so
     * collapsing them avoids a redundant second click for information
     * nobody outside this service needs to know arrived separately.
     */
    public void acceptedByMerchant(UUID orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.CONFIRMED) {
            return;
        }
        order.transitionTo(OrderStatus.MERCHANT_ACCEPTED);
        order.transitionTo(OrderStatus.PREPARING);
        orderRepository.save(order);
    }

    /** A merchant rejection after payment succeeded needs the same compensating actions as a customer cancel — release the reservation, refund via the same order.cancelled -> payment-service path (§8's REST-vs-Kafka rule still applies: order-service doesn't need the refund's result to decide anything next). */
    public void rejectedByMerchant(UUID orderId, String reason) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.CONFIRMED) {
            return;
        }
        order.transitionTo(OrderStatus.MERCHANT_REJECTED);
        order.setCancellationReason(reason);
        Order saved = orderRepository.save(order);

        inventoryClient.release(orderId);
        eventPublisher.publishCancelled(saved.getId(), saved.getUserId(), saved.getTotalAmount());
    }

    /** From merchant-service's order.ready event — the merchant has finished preparing. delivery-service consumes the same event directly off order-events to trigger assignment; this just keeps order-service's own canonical status current. */
    public void markReadyForPickup(UUID orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PREPARING) {
            return;
        }
        order.transitionTo(OrderStatus.READY_FOR_PICKUP);
        orderRepository.save(order);
    }

    /** From delivery-service's delivery.assigned event. */
    public void assignedToDelivery(UUID orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.READY_FOR_PICKUP) {
            return;
        }
        order.transitionTo(OrderStatus.DELIVERY_ASSIGNED);
        orderRepository.save(order);
    }

    /**
     * From delivery-service's delivery.delivered event. Two transitions in
     * one call — DELIVERY_ASSIGNED -> OUT_FOR_DELIVERY -> DELIVERED — same
     * double-hop reasoning as acceptedByMerchant: BUILD_TRACKER's Stage 11
     * scope only asks for delivery.assigned/delivery.delivered, with no
     * separate "picked up, now en route" signal to react to individually.
     */
    public void delivered(UUID orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.DELIVERY_ASSIGNED) {
            return;
        }
        order.transitionTo(OrderStatus.OUT_FOR_DELIVERY);
        order.transitionTo(OrderStatus.DELIVERED);
        orderRepository.save(order);
    }

    public Order getById(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!order.getUserId().equals(userId)) {
            throw new ForbiddenException("This order does not belong to you");
        }
        return order;
    }

    public List<Order> myOrders(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Only reachable, pre-delivery states per BUILD_TRACKER's "cancel order
     * endpoint (pre-delivery states only)" — the transition guard itself
     * enforces this (CREATED/CONFIRMED/PREPARING -> CANCELLED is legal,
     * everything past READY_FOR_PICKUP is not). Inventory release is a
     * direct synchronous call (inventory-service doesn't consume
     * order-events — §7's table doesn't list it as a dependency); the
     * refund is deliberately event-driven instead (order.cancelled ->
     * payment-service's consumer) per §8's REST-vs-Kafka rule: order-service
     * doesn't need the refund's result to decide anything next. The DB
     * commit happens first and fully completes before either the REST call
     * or the Kafka publish, same reasoning as placeOrder.
     */
    public Order cancel(UUID orderId, UUID userId, String reason) {
        Order order = getById(orderId, userId);
        order.transitionTo(OrderStatus.CANCELLED);
        order.setCancellationReason(reason);
        Order saved = orderRepository.save(order);

        inventoryClient.release(order.getId());
        eventPublisher.publishCancelled(order.getId(), order.getUserId(), order.getTotalAmount());

        return saved;
    }

    // --- small, single-save steps (see class comment for why each is exactly one save() call) ---

    Order createOrder(UUID userId, CartClient.CartView cart) {
        Order order = new Order(userId, cart.merchantId(), cart.subtotal());
        for (CartClient.CartLine line : cart.items()) {
            order.addItem(new OrderItem(line.listingId(), line.productId(), line.productName(), line.price(), line.quantity()));
        }
        return orderRepository.save(order);
    }

    Order applyCoupon(Order order, String couponCode, BigDecimal discount) {
        order.applyCoupon(couponCode, discount);
        return orderRepository.save(order);
    }

    Order cancelOrder(Order order, String reason) {
        order.transitionTo(OrderStatus.CANCELLED);
        order.setCancellationReason(reason);
        return orderRepository.save(order);
    }

    Order markPaymentPending(Order order) {
        order.transitionTo(OrderStatus.PAYMENT_PENDING);
        return orderRepository.save(order);
    }

    Order markPaymentFailed(Order order) {
        order.transitionTo(OrderStatus.PAYMENT_FAILED);
        return orderRepository.save(order);
    }

    Order markConfirmed(Order order) {
        order.transitionTo(OrderStatus.CONFIRMED);
        return orderRepository.save(order);
    }
}
