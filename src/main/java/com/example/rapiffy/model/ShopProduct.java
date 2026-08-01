package com.example.rapiffy.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ShopProduct is the Admin's (shopkeeper's) actual product listing.
 *
 * TWO ways a ShopProduct is created:
 *
 * 1. FROM CATALOG: Admin activates a MasterProduct → ShopProduct created with masterProduct linked.
 *    Admin can override name, price, description, etc. If not overridden, defaults from MasterProduct are used.
 *
 * 2. UNLISTED/CUSTOM: Admin adds a product not in catalog → ShopProduct created with masterProduct = NULL.
 *    Admin must fill all fields manually.
 *
 * This is what the CUSTOMER sees when browsing a shop.
 */
@Entity
@Table(name = "shop_products")
@Data
public class ShopProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── SHOP REFERENCE ──────────────────────────────────────────────────────

    // Which shop (Admin) owns this product listing
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Profile shop;

    // ─── CATALOG REFERENCE (nullable for unlisted products) ──────────────────

    // Linked MasterProduct — NULL means this is an unlisted/custom product
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_product_id")
    private MasterProduct masterProduct;

    // ─── PRODUCT DETAILS (Admin can override or fill for unlisted) ───────────

    // Product name — override from MasterProduct OR custom name for unlisted
    @Column(name = "product_name")
    private String productName;

    // Short description (e.g. "Premium basmati rice")
    @Column(name = "short_description")
    private String shortDescription;

    // Long description with full details
    @Column(name = "long_description", columnDefinition = "TEXT")
    private String longDescription;

    // Product image URL — override or custom
    @Column(name = "image_url")
    private String imageUrl;

    // Brand name — override or custom
    @Column(name = "brand")
    private String brand;

    // ─── PRICING ─────────────────────────────────────────────────────────────

    // Maximum Retail Price (printed on product)
    @Column(name = "mrp")
    private Double mrp;

    // Admin's selling price (can be less than or equal to MRP)
    @Column(name = "selling_price", nullable = false)
    private Double sellingPrice;

    // ─── QUANTITY & STOCK ────────────────────────────────────────────────────

    // How many units Admin currently has in stock
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    // Alert threshold — notify Admin when stock falls below this
    @Column(name = "threshold_quantity")
    private Integer thresholdQuantity;

    // ─── MEASUREMENT ─────────────────────────────────────────────────────────

    // Unit of measurement (e.g. "KG", "ML", "PCS", "METER", "CM")
    @Column(name = "unit")
    private String unit;

    // Value of unit (e.g. "5" for 5KG, "500" for 500ML)
    @Column(name = "unit_value")
    private String unitValue;

    // ─── EXPIRY (important for Grocery, Medical, Food) ───────────────────────

    // Product expiry date — relevant for perishable goods
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    // ─── VARIANTS ────────────────────────────────────────────────────────

    // Does this product have multiple variants? (default: false)
    // false = single product, price/stock on this ShopProduct itself
    // true  = multiple variants, price/stock on each ProductVariant
    @Column(name = "has_variants", nullable = false)
    private boolean hasVariants = false;

    // List of variants (only used when hasVariants = true)
    @OneToMany(mappedBy = "shopProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<ProductVariant> variants = new java.util.ArrayList<>();

    // ─── STATUS FLAGS ────────────────────────────────────────────────────────

    // true = Admin is selling this product, false = deactivated/not selling
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // SubCategory — set for both catalog and unlisted products
    // Category is derived via subCategory.getCategory()
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_category_id")
    private SubCategory subCategory;

    private String gstSlab;

    // ─── TIMESTAMPS ──────────────────────────────────────────────────────────

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
