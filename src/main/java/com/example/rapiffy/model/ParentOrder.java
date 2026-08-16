package com.example.rapiffy.model;

import com.example.rapiffy.enums.OrderStatus;
import com.example.rapiffy.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ParentOrder — the single order a customer places.
 *
 * Internally split into multiple sub-orders (Order) — one per shop.
 * Customer sees 1 order, pays once.
 * Each shop admin sees only their own sub-order (Order).
 *
 * Example:
 *   ParentOrder #PO-001 (customer pays ₹850 total)
 *   ├── Order (sub) → Grocery Shop  → Rice, Dal       ₹500
 *   ├── Order (sub) → Medical Shop  → Paracetamol     ₹150
 *   └── Order (sub) → Dairy Shop    → Milk, Curd      ₹200
 */
@Entity
@Table(name = "parent_orders")
@Data
public class ParentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Human-readable order number shown to customer (e.g. "PO-20240719-0001")
    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber;

    // Customer who placed the order
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    // Sub-orders — one per shop
    @OneToMany(mappedBy = "parentOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> subOrders = new ArrayList<>();

    // ─── DELIVERY (shared across all sub-orders) ─────────────────────────────

    @Column(name = "delivery_type")
    private String deliveryType;           // DELIVERY or SELF

    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;        // same address for all shops

    @Column(name = "delivery_instruction", columnDefinition = "TEXT")
    private String deliveryInstruction;    // optional note for delivery person

    // ─── PRICING SUMMARY (sum of all sub-orders) ─────────────────────────────

    @Column(name = "subtotal", nullable = false)
    private Double subtotal;

    @Column(name = "total_gst")
    private Double totalGst;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    // ─── PAYMENT ─────────────────────────────────────────────────────────────

    // Payment status for this order (tracks payment lifecycle separately from order status)
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 25)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private com.example.rapiffy.enums.PaymentMethod paymentMethod;

    // Total amount refunded so far (sum of all sub-order refunds)
    @Column(name = "refunded_amount", nullable = false)
    private Double refundedAmount = 0.0;

    // ─── OVERALL STATUS ──────────────────────────────────────────────────────
    // PAYMENT_PENDING → PENDING (after payment) → tracks based on sub-order statuses

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status = OrderStatus.PAYMENT_PENDING;

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
