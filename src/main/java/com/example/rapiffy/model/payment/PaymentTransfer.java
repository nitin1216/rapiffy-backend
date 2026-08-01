package com.example.rapiffy.model.payment;

import com.example.rapiffy.enums.TransferStatus;
import com.example.rapiffy.model.Order;
import com.example.rapiffy.model.Profile;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * PaymentTransfer — tracks money split from platform to each shop via Razorpay Route.
 *
 * After customer payment is captured, one transfer is created per sub-order (per shop).
 * This routes the shop's share of the total payment directly to their linked bank account.
 *
 * Example:
 *   Payment (₹850 captured)
 *   ├── Transfer → Shop A (₹500) — linked_account_acc_xxx
 *   ├── Transfer → Shop B (₹150) — linked_account_acc_yyy
 *   └── Transfer → Shop C (₹200) — linked_account_acc_zzz
 */
@Entity
@Table(name = "payment_transfers")
@Data
public class PaymentTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── REFERENCES ──────────────────────────────────────────────────────────

    // Parent payment this transfer belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    // Which sub-order this transfer is for
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Shop (Admin) receiving this transfer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Profile shop;

    // ─── RAZORPAY ROUTE DETAILS ──────────────────────────────────────────────

    // Razorpay transfer ID (e.g. "trf_xxxxx")
    @Column(name = "razorpay_transfer_id", unique = true)
    private String razorpayTransferId;

    // Shop's Razorpay linked account ID (e.g. "acc_xxxxx")
    @Column(name = "razorpay_linked_account_id")
    private String razorpayLinkedAccountId;

    // ─── AMOUNT ──────────────────────────────────────────────────────────────

    // Amount transferred to this shop (= sub-order totalAmount minus platform commission)
    @Column(name = "amount", nullable = false)
    private Double amount;

    // Platform commission deducted from customer side (e.g. 2.5%)
    @Column(name = "platform_commission", nullable = false)
    private Double platformCommission = 0.0;

    // Shop commission deducted from shop payout based on category (e.g. Grocery 3%)
    @Column(name = "shop_commission", nullable = false)
    private Double shopCommission = 0.0;

    // ─── STATUS ──────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransferStatus status = TransferStatus.PENDING;

    // ─── TIMESTAMPS ──────────────────────────────────────────────────────────

    @Column(name = "transferred_at")
    private LocalDateTime transferredAt;   // when Razorpay created the transfer

    @Column(name = "settled_at")
    private LocalDateTime settledAt;       // when money reached shop's bank

    @Column(name = "reversed_at")
    private LocalDateTime reversedAt;      // if transfer was reversed (cancellation)

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
