package com.example.rapiffy.model.payment;

import com.example.rapiffy.enums.CancelledBy;
import com.example.rapiffy.enums.RefundStatus;
import com.example.rapiffy.model.Order;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Refund — tracks a refund issued against a payment.
 *
 * Refunds are always at the sub-order level:
 * - Customer cancels one sub-order → partial refund of that sub-order's amount
 * - Admin rejects one sub-order → partial refund of that sub-order's amount
 * - All sub-orders cancelled → effectively a full refund (sum of all partial refunds)
 *
 * Each refund is linked to a specific sub-order (Order) so we know
 * which shop's transfer needs to be reversed.
 *
 * Example:
 *   Payment (₹850)
 *   └── Refund → Sub-order Shop B cancelled → ₹150 refunded to customer
 */
@Entity
@Table(name = "refunds")
@Data
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── REFERENCES ──────────────────────────────────────────────────────────

    // Parent payment this refund is against
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    // Which sub-order was cancelled/refunded (nullable for full-order refunds)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    // ─── RAZORPAY DETAILS ────────────────────────────────────────────────────

    // Razorpay refund ID (e.g. "rfnd_xxxxx")
    @Column(name = "razorpay_refund_id", unique = true)
    private String razorpayRefundId;

    // ─── AMOUNT ──────────────────────────────────────────────────────────────

    // Refund amount (= sub-order totalAmount being refunded)
    @Column(name = "amount", nullable = false)
    private Double amount;

    // ─── REASON ──────────────────────────────────────────────────────────────

    // Who initiated the cancellation that triggered this refund
    @Enumerated(EnumType.STRING)
    @Column(name = "cancelled_by", nullable = false, length = 20)
    private CancelledBy cancelledBy;

    // Human-readable reason (e.g. "Customer changed mind", "Out of stock")
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    // ─── STATUS ──────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RefundStatus status = RefundStatus.INITIATED;

    // ─── TIMESTAMPS ──────────────────────────────────────────────────────────

    @Column(name = "initiated_at", updatable = false)
    private LocalDateTime initiatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;     // when Razorpay confirms refund

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.initiatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
