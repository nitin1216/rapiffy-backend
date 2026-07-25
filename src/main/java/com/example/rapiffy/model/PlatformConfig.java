package com.example.rapiffy.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * PlatformConfig — stores global platform settings managed by SuperAdmin.
 *
 * Single-row table (only one config record exists).
 * Admin (shopkeeper) has NO access to this — completely internal.
 *
 * Used for:
 * - Default platform commission rate
 * - Cancellation window (how long customer can cancel after payment)
 * - Transfer delay (buffer before money is transferred to shops)
 */
@Entity
@Table(name = "platform_config")
@Data
public class PlatformConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── COMMISSION ──────────────────────────────────────────────────────────

    // Default platform commission percentage (e.g. 5.0 = 5%)
    // This is what platform earns per transaction from each shop
    // Admin (shopkeeper) NEVER sees this value
    @Column(name = "default_commission_rate", nullable = false)
    private Double defaultCommissionRate = 5.0;

    // ─── CANCELLATION & TRANSFER TIMING ──────────────────────────────────────

    // Minutes after payment within which customer can cancel (default: 30 min)
    // During this window, transfers are not yet created → easy refund
    @Column(name = "cancellation_window_minutes", nullable = false)
    private Integer cancellationWindowMinutes = 30;

    // Minutes after payment before transfers are created to shops (default: 60 min)
    // Should be >= cancellationWindowMinutes
    @Column(name = "transfer_delay_minutes", nullable = false)
    private Integer transferDelayMinutes = 60;

    // ─── TIMESTAMPS ──────────────────────────────────────────────────────────

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
