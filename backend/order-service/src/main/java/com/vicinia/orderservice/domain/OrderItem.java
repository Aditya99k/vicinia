package com.vicinia.orderservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/** A line-item snapshot taken at order-placement time from cart-service's own live-priced response — unlike the cart, an order's line prices are fixed once placed, an immutable record of what was actually charged. */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    private UUID listingId;
    private String productId;
    private String productName;

    @Column(precision = 10, scale = 2)
    private BigDecimal unitPrice;

    private int quantity;

    @Column(precision = 10, scale = 2)
    private BigDecimal lineTotal;

    protected OrderItem() {
    }

    public OrderItem(UUID listingId, String productId, String productName, BigDecimal unitPrice, int quantity) {
        this.listingId = listingId;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    void setOrder(Order order) {
        this.order = order;
    }

    public UUID getId() {
        return id;
    }

    public UUID getListingId() {
        return listingId;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }
}
