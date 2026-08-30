package com.example.rapiffy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * MasterProductVariant is treated as a first-class product in the global catalog.
 *
 * SuperAdmin adds these variants separately under a MasterProduct.
 * subCategory and category are inherited from the parent MasterProduct.
 *
 * Example:
 *   Parent MasterProduct = "Cooking Oil" (hasVariants = true)
 *     Variant 1: Fortune Sunflower 1L  → parentMasterProductId = 10
 *     Variant 2: Saffola Gold 2L       → parentMasterProductId = 10
 */
@Entity
@Table(name = "master_product_variants")
@Data
public class MasterProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Parent MasterProduct this variant belongs to
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_master_product_id", nullable = false)
    private MasterProduct parentMasterProduct;

    // ─── PRODUCT FIELDS (same as MasterProduct) ──────────────────────────────

    @Column(name = "variant_name")
    private String variantName;

    @Column(name = "brand")
    private String brand;

    @Column(name = "short_description")
    private String shortDescription;

    @Column(name = "long_description", columnDefinition = "TEXT")
    private String longDescription;

    @Column(name = "mrp")
    private Double mrp;

    @Column(name = "selling_price")
    private Double sellingPrice;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @Column(name = "threshold_quantity")
    private Integer thresholdQuantity;

    // Thumbnail image URL (set automatically on first image upload)
    @Column(name = "image_url")
    private String imageUrl;

    // Full image gallery
    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<MasterProductVariantImage> images = new java.util.ArrayList<>();
    private LocalDate expiryDate;

    @Column(name = "gst_slab")
    private String gstSlab;

    // Attribute values for this variant (e.g. Size=8, Colour=Red)
    @OneToMany(mappedBy = "masterProductVariant", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<VariantAttributeValue> attributeValues = new java.util.ArrayList<>();

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

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
