package com.example.rapiffy.model;

import jakarta.persistence.*;
import lombok.Data;

import java.awt.image.BufferedImage;

@Entity
@Table(name = "products")
@Data
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", unique = true, nullable = false)
    private String productCode;
    @ManyToOne
    @JoinColumn(name = "categoryCode")
    private Long categoryCode; // category code --> foreign key of category code

    @Embedded
    private String productName;

    @Embedded
    private BufferedImage productImage; // JPG, PNG

    @Embedded
    private int size; // KG, ML, CM

    @Embedded
    private double price;

    @Embedded
    private int rating;

    @Embedded
    private String description;
}
