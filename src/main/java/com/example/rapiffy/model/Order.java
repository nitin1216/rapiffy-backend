package com.example.rapiffy.model;

import com.example.rapiffy.enums.CancelledBy;
import com.example.rapiffy.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Order — a sub-order for a specific Shop (Admin).
 *
 * Created internally when a customer places a ParentOrder.
 * Each shop gets their own Order with only their products.
 *
 * Flow: PAYMENT_PENDING → PENDING → CONFIRMED → READY → OUT_FOR_DELIVERY → DELIVERED
 *       PENDING → CANCELLED (by customer)
 *       PENDING → REJECTED (by admin)
 */
@Entity
@Table(name = "orders")
@Data
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Sub-order number (e.g. "PO-20240719-0001-S1")
    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber;

    // ─── REFERENCES ──────────────────────────────────────────────────────────

    // Parent order this sub-order belongs to
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_order_id")
    private ParentOrder parentOrder;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Profile shop;

    // ─── ORDER ITEMS ─────────────────────────────────────────────────────────

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    // ─── PRICING SUMMARY ─────────────────────────────────────────────────────

    @Column(name = "subtotal", nullable = false)
    private Double subtotal;           // sum of (sellingPrice * qty) before GST

    @Column(name = "total_gst")
    private Double totalGst;           // total GST amount

    @Column(name = "delivery_charge")
    private Double deliveryCharge = 0.0;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;        // subtotal + totalGst + deliveryCharge

    // ─── DELIVERY ────────────────────────────────────────────────────────────

    @Column(name = "delivery_type")    // "SELF" or "DELIVERY"
    private String deliveryType;

    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    // ─── STATUS ──────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status = OrderStatus.PAYMENT_PENDING;

    // ─── CANCELLATION ────────────────────────────────────────────────────────

    // Who cancelled this sub-order (null if not cancelled)
    @Enumerated(EnumType.STRING)
    @Column(name = "cancelled_by", length = 20)
    private CancelledBy cancelledBy;

    // Reason for cancellation (e.g. "Customer changed mind", "Out of stock")
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    // When the sub-order was cancelled
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    // Whether refund has been processed for this sub-order
    @Column(name = "is_refunded", nullable = false)
    private boolean isRefunded = false;

    // ─── INVOICE ─────────────────────────────────────────────────────────────

    @Column(name = "invoice_id")
    private String invoiceId;          // generated after order is confirmed

    // ─── TIMESTAMPS ──────────────────────────────────────────────────────────

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
