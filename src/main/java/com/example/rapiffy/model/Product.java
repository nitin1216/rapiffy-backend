package com.example.rapiffy.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "products")
@Data
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", unique = true, nullable = false)
    private String productCode;

    // Many products belong to one category
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "product_name", nullable = false)
    private String productName;

    // Store image as binary (JPG/PNG). Use @Lob for large binary data.
    @Lob
    @Column(name = "product_image")
    private byte[] productImage;

    @Column(name = "size")
    private String size; // e.g. "1 KG", "500 ML", "30 CM"

    @Column(name = "price", nullable = false)
    private double price;

    @Column(name = "rating")
    private int rating;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
