package com.example.rapiffy.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MasterProduct is the GLOBAL product catalog managed by SuperAdmin.
 *
 * - SuperAdmin imports products via CSV (bulk) into this table
 * - Each product belongs to a Category
 * - Admin (shopkeeper) sees these products and activates the ones they sell
 * - Contains default values (name, MRP, unit) that Admin can override in ShopProduct
 *
 * Example: "India Gate Basmati Rice 5KG" with MRP 500 exists here.
 *          Multiple shops can activate this product with their own price.
 */
@Entity
@Table(name = "master_products")
@Data
public class MasterProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique product code for identification (e.g. "GRO-00001")
    @Column(name = "product_code", unique = true, nullable = false)
    private String productCode;

    // Default product name (Admin can override in ShopProduct)
    @Column(name = "product_name", nullable = false)
    private String productName;

    // Which category this product belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // Brand name (e.g. "India Gate", "Tata", "Amul")
    @Column(name = "brand")
    private String brand;

    // Measurement unit (e.g. "KG", "ML", "PCS", "METER", "CM")
    @Column(name = "unit")
    private String unit;

    // Value of the unit (e.g. "5" for 5KG, "500" for 500ML)
    @Column(name = "unit_value")
    private String unitValue;

    // Maximum Retail Price — the printed MRP on product
    @Column(name = "mrp")
    private Double mrp;

    // Product image URL
    @Column(name = "image_url")
    private String imageUrl;

    // Short one-line description (e.g. "Premium aged basmati rice")
    @Column(name = "short_description")
    private String shortDescription;

    // Detailed description with full info
    @Column(name = "long_description", columnDefinition = "TEXT")
    private String longDescription;


    // Does this product have multiple variants? (default: false)
    // SuperAdmin sets this when product has multiple sizes/brands/types
    @Column(name = "has_variants", nullable = false)
    private boolean hasVariants = false;

    // Default variants (only when hasVariants = true)
    @OneToMany(mappedBy = "masterProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<MasterProductVariant> variants = new java.util.ArrayList<>();

    // SuperAdmin can deactivate a product globally
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
