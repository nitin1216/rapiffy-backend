package com.example.rapiffy.model;

import com.example.rapiffy.common.CAddress;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CustomerAddress — one saved address for a customer.
 * Customer can have multiple addresses (Home, Office, Other).
 * Only one address can be isDefault = true at a time.
 * Default address is auto-used at order placement if no address is provided.
 */
@Entity
@Table(name = "customer_addresses")
@Data
public class CustomerAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    // Label — e.g. "Home", "Office", "Other"
    @Column(name = "label", length = 50)
    private String label;

    @Embedded
    private CAddress address;

    // Only one address can be default at a time
    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
