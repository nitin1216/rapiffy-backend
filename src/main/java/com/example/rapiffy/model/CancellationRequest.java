package com.example.rapiffy.model;

import com.example.rapiffy.enums.CancellationStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * CancellationRequest — raised by customer when they want to cancel specific items
 * from a sub-order that is still in PENDING status.
 * Admin must approve/reject before any refund is processed.
 */
@Entity
@Table(name = "cancellation_requests", indexes = {
        @Index(name = "idx_cancel_order",    columnList = "order_id"),
        @Index(name = "idx_cancel_customer", columnList = "customer_id"),
        @Index(name = "idx_cancel_status",   columnList = "status")
})
@Data
public class CancellationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Column(name = "reason", columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CancellationStatus status = CancellationStatus.REQUESTED;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    // Total refund amount = sum of CancellationItem.lineRefundAmount
    @Column(name = "refund_amount", nullable = false)
    private Double refundAmount = 0.0;

    @OneToMany(mappedBy = "cancellationRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CancellationItem> items = new ArrayList<>();

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
