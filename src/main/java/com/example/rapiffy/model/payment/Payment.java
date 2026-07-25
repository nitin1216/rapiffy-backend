package com.example.rapiffy.model.payment;

import com.example.rapiffy.enums.PaymentMethod;
import com.example.rapiffy.enums.PaymentStatus;
import com.example.rapiffy.model.ParentOrder;
import com.example.rapiffy.model.User;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Payment — tracks the single payment a customer makes for a ParentOrder.
 *
 * One ParentOrder = one Payment (customer pays once for all shops).
 * After payment is captured, transfers are created to split money to each shop.
 *
 * Lifecycle: PENDING → PAID → (PARTIALLY_REFUNDED / FULLY_REFUNDED)
 *            PENDING → FAILED (if payment attempt fails)
 */
@Entity
@Table(name = "payments")
@Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── REFERENCES ──────────────────────────────────────────────────────────

    // The parent order this payment is for
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_order_id", nullable = false, unique = true)
    private ParentOrder parentOrder;

    // Customer who made the payment
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    // ─── RAZORPAY DETAILS ────────────────────────────────────────────────────

    // Razorpay order ID (created when customer initiates payment)
    @Column(name = "razorpay_order_id", unique = true)
    private String razorpayOrderId;

    // Razorpay payment ID (received after successful payment)
    @Column(name = "razorpay_payment_id", unique = true)
    private String razorpayPaymentId;

    // Razorpay signature (for verification)
    @Column(name = "razorpay_signature")
    private String razorpaySignature;

    // ─── AMOUNT ──────────────────────────────────────────────────────────────

    // Total amount charged (in rupees) — same as ParentOrder.totalAmount
    @Column(name = "amount", nullable = false)
    private Double amount;

    // Amount refunded so far (starts at 0, increases with each refund)
    @Column(name = "refunded_amount", nullable = false)
    private Double refundedAmount = 0.0;

    // Currency code (default INR)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "INR";

    // ─── PAYMENT INFO ────────────────────────────────────────────────────────

    // How customer paid (UPI, CARD, NETBANKING, WALLET, COD)
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    // Payment status
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 25)
    private PaymentStatus status = PaymentStatus.PENDING;

    // ─── TRANSFERS (split to shops) ──────────────────────────────────────────

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentTransfer> transfers = new ArrayList<>();

    // ─── REFUNDS ─────────────────────────────────────────────────────────────

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Refund> refunds = new ArrayList<>();

    // ─── TIMESTAMPS ──────────────────────────────────────────────────────────

    @Column(name = "paid_at")
    private LocalDateTime paidAt;          // when payment was captured

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
