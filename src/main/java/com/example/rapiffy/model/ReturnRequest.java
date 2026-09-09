package com.example.rapiffy.model;

import com.example.rapiffy.enums.ReturnReason;
import com.example.rapiffy.enums.ReturnStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ReturnRequest — raised by customer after order is DELIVERED.
 * Linked to a sub-order (Order). Customer selects items, picks a reason, uploads images.
 * Admin reviews and approves/rejects. On approval, refund goes to customer's Wallet.
 */
@Entity
@Table(name = "return_requests", indexes = {
        @Index(name = "idx_return_order",    columnList = "order_id"),
        @Index(name = "idx_return_customer", columnList = "customer_id"),
        @Index(name = "idx_return_status",   columnList = "status")
})
@Data
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Sub-order this return is against
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 20)
    private ReturnReason reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReturnStatus status = ReturnStatus.REQUESTED;

    // Customer's description of the issue
    @Column(name = "customer_note", columnDefinition = "TEXT")
    private String customerNote;

    // Admin's note when approving or rejecting
    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    // Total refund amount = sum of ReturnItem.lineRefundAmount
    @Column(name = "refund_amount", nullable = false)
    private Double refundAmount = 0.0;

    // Items selected for return
    @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReturnItem> items = new ArrayList<>();

    // Proof images uploaded by customer
    @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReturnImage> images = new ArrayList<>();

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
