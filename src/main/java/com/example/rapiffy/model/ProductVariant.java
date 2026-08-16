package com.example.rapiffy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ProductVariant is treated as a first-class product.
 *
 * Every variant has its own unique shopProductId — so it can be
 * carted and ordered exactly like a regular ShopProduct.
 *
 * parentShopProduct links it back to the parent ShopProduct for grouping/display.
 * subCategory and category are inherited from the parent ShopProduct.
 *
 * Example:
 *   Parent ShopProduct = "Cooking Oil" (shopProductId = 101, hasVariants = true)
 *     Variant 1: Fortune 1L  → shopProductId = 301, parentShopProductId = 101
 *     Variant 2: Saffola 2L  → shopProductId = 302, parentShopProductId = 101
 */
@Entity
@Table(name = "product_variants")
@Data
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique product identity — auto-generated, used in cart/order just like ShopProduct.id
    @Column(name = "shop_product_id", unique = true)
    private Long shopProductId;

    // Parent ShopProduct this variant belongs to
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_shop_product_id", nullable = false)
    private ShopProduct parentShopProduct;

    // ─── PRODUCT FIELDS (same as ShopProduct) ────────────────────────────────

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

    @Column(name = "gst_slab")
    private String gstSlab;

    // Attribute values for this variant (e.g. Size=8, Colour=Red)
    @OneToMany(mappedBy = "productVariant", cascade = CascadeType.ALL, orphanRemoval = true)
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
