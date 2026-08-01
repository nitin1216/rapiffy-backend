package com.example.rapiffy.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * SubCategory sits between Category and MasterProduct.
 *
 * Hierarchy: Category → SubCategory → MasterProduct → ProductVariant
 *
 * Example:
 *   Category: Grocery
 *     SubCategory: Rice
 *       MasterProduct: India Gate Basmati
 *         Variant: 1KG, 2KG, 5KG
 *
 * - Created and managed by SUPER_ADMIN
 * - MasterProduct and ShopProduct reference SubCategory (not Category directly)
 * - Category is derived via subCategory.getCategory()
 */
@Entity
@Table(name = "sub_categories")
@Data
@EqualsAndHashCode(of = "id")
public class SubCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "description")
    private String description;

    // Parent category this subCategory belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
