package com.example.rapiffy.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ProductVariant represents one variant of a ShopProduct.
 *
 * Example: ShopProduct = "Cooking Oil" (hasVariants = true)
 *   Variant 1: Fortune Sunflower, 1L, ₹180, stock=20
 *   Variant 2: Fortune Sunflower, 5L, ₹850, stock=10
 *   Variant 3: Saffola Gold, 1L, ₹210, stock=15
 *
 * Each variant is independently priced, stocked, and can be toggled on/off.
 */
@Entity
@Table(name = "product_variants")
@Data
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Parent ShopProduct this variant belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_product_id", nullable = false)
    private ShopProduct shopProduct;

    // Variant-specific details
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

    @Column(name = "selling_price", nullable = false)
    private Double sellingPrice;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Column(name = "threshold_quantity")
    private Integer thresholdQuantity;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
