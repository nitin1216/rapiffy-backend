package com.example.rapiffy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MasterProductVariant represents a default variant in the global catalog.
 *
 * SuperAdmin adds these when importing/managing MasterProducts.
 * When Admin activates a product, they can see these default variants
 * and choose which ones to sell (or add their own).
 *
 * Example: MasterProduct = "Cooking Oil" (hasVariants = true)
 *   Variant 1: Fortune Sunflower, 1L, MRP ₹180
 *   Variant 2: Fortune Sunflower, 5L, MRP ₹850
 *   Variant 3: Saffola Gold, 1L, MRP ₹210
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
    @JoinColumn(name = "master_product_id", nullable = false)
    private MasterProduct masterProduct;

    @Column(name = "variant_name")
    private String variantName; // e.g. "Fortune Sunflower Oil 1L"

    @Column(name = "brand")
    private String brand;

    @Column(name = "unit")
    private String unit; // KG, ML, PCS, CM

    @Column(name = "unit_value")
    private String unitValue; // 1, 5, 500

    @Column(name = "mrp")
    private Double mrp;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
