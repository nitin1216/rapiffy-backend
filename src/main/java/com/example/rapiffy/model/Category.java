package com.example.rapiffy.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Category represents the type of shop/business.
 * Examples: Grocery, Medical, Fashion, Dairy, Electronics
 *
 * - Created and managed by SUPER_ADMIN
 * - Admin (shopkeeper) selects one or more categories for their shop
 * - MasterProducts are organized under categories
 */
@Entity
@Table(name = "categories")
@Data
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique code for internal reference (e.g. "GRO", "MED", "FSH")
    @Column(name = "category_code", unique = true, nullable = false)
    private String categoryCode;

    // Display name shown to Admin & Customer (e.g. "Grocery", "Medical")
    @Column(name = "category_name", nullable = false)
    private String categoryName;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "description")
    private String description;

    // SuperAdmin can disable a category — hides it from all shops
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
