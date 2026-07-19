package com.example.rapiffy.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * OrderItem — one line in an Order.
 * Snapshot of product details at the time of order (price/name may change later).
 */
@Entity
@Table(name = "order_items")
@Data
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Reference to the shop product (nullable — product may be deleted later)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_product_id")
    private ShopProduct shopProduct;

    // ─── SNAPSHOT at time of order ───────────────────────────────────────────

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "brand")
    private String brand;

    @Column(name = "unit")
    private String unit;

    @Column(name = "unit_value")
    private String unitValue;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "mrp")
    private Double mrp;

    @Column(name = "selling_price", nullable = false)
    private Double sellingPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "gst_slab")
    private String gstSlab;            // e.g. "5%", "12%", "18%"

    @Column(name = "gst_amount")
    private Double gstAmount;          // calculated: sellingPrice * qty * gstRate

    @Column(name = "line_total", nullable = false)
    private Double lineTotal;          // sellingPrice * quantity + gstAmount
}
