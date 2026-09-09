package com.example.rapiffy.model;

import com.example.rapiffy.enums.ShopDeductionStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ShopDeduction — records every amount deducted from a shop's payout due to a return.
 *
 * When a return is approved (COD or Online), the refund amount is deducted from
 * the shop's next transfer payout. This table provides a full audit trail so the
 * shop owner can see exactly why their payout was reduced.
 *
 * Example:
 *   Customer returns ₹150 item from Shop A
 *   → ShopDeduction created: shop=ShopA, amount=₹150, status=PENDING
 *   → Next order for ShopA: transfer = ₹500 - ₹25 commission - ₹150 deduction = ₹325
 *   → ShopDeduction status updated to SETTLED, settledViaOrderId = new order id
 */
@Entity
@Table(name = "shop_deductions", indexes = {
        @Index(name = "idx_shop_deduction_shop",   columnList = "shop_id"),
        @Index(name = "idx_shop_deduction_status", columnList = "status")
})
@Data
public class ShopDeduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Shop whose payout will be reduced
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Profile shop;

    // Return request that caused this deduction
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_request_id", nullable = false)
    private ReturnRequest returnRequest;

    // The sub-order that was returned
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Amount to deduct from shop's next payout
    @Column(name = "amount", nullable = false)
    private Double amount;

    // Human-readable reason shown to shop owner
    @Column(name = "reason", columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ShopDeductionStatus status = ShopDeductionStatus.PENDING;

    // Which order's transfer this deduction was settled against (set when SETTLED)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settled_via_order_id")
    private Order settledViaOrder;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

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
