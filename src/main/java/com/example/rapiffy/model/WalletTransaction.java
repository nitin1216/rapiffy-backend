package com.example.rapiffy.model;

import com.example.rapiffy.enums.WalletTransactionType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * WalletTransaction — ledger entry for every wallet credit/debit.
 * source describes why the transaction happened (e.g. "RETURN_REFUND", "ORDER_PAYMENT").
 */
@Entity
@Table(name = "wallet_transactions", indexes = {
        @Index(name = "idx_wallet_txn_user", columnList = "user_id")
})
@Data
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private WalletTransactionType type;

    // e.g. "RETURN_REFUND", "ORDER_PAYMENT"
    @Column(name = "source", nullable = false, length = 30)
    private String source;

    @Column(name = "amount", nullable = false)
    private Double amount;

    // ID of the entity that triggered this transaction (e.g. returnRequest.id)
    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
