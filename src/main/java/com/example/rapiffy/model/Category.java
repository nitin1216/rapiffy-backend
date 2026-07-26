package com.example.rapiffy.model;

import com.example.rapiffy.enums.CategoryType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
@EqualsAndHashCode(of = "id")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Enum type — used by SUPERADMIN to select categories for an Admin
    @Enumerated(EnumType.STRING)
    @Column(name = "category_type", unique = true, nullable = false, length = 50)
    private CategoryType categoryType;

    @Column(name = "image_url")
    private String imageUrl;

    @Transient
    public String getCategoryName() { return categoryType != null ? categoryType.display() : null; }

    @Transient
    public String getCategoryCode() { return categoryType != null ? categoryType.name() : null; }

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
