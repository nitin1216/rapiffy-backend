package com.example.rapiffy.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * PlatformCommission — per-CATEGORY commission slab.
 *
 * Internal platform config. Admin (shopkeeper) has NO visibility.
 * Each category has its own commission rate.
 *
 * Example:
 *   Grocery     → 3%  (low margin, high volume)
 *   Medical     → 2%  (essential, keep low)
 *   Dairy       → 3%
 *   Fashion     → 10% (high margin products)
 *   Electronics → 5%
 *   (any category without entry) → uses PlatformConfig.defaultCommissionRate
 *
 * How it works:
 *   When splitting payment to shops, each ORDER ITEM's commission is calculated
 *   based on its product's category. A single sub-order may have items from
 *   multiple categories — commission is applied per-item, then summed up.
 */
@Entity
@Table(name = "platform_commissions",
       uniqueConstraints = @UniqueConstraint(columnNames = "category_id"))
@Data
public class PlatformCommission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which category this commission rate applies to
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false, unique = true)
    private Category category;

    // Commission rate for this category (e.g. 3.0 = 3%)
    @Column(name = "commission_rate", nullable = false)
    private Double commissionRate;

    // Optional note (e.g. "Low rate for essential goods")
    @Column(name = "notes")
    private String notes;

    // Is this commission slab active?
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

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
