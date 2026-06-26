package com.example.rapiffy.model;

import com.example.rapiffy.enums.CategoryStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.awt.image.BufferedImage;

@Entity
@Table(name = "categories")
@Data
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_code", unique = true, nullable = false)
    private String categoryCode;

    @Embedded
    private String categoryName;

    @Embedded
    private CategoryStatus status;

    @Embedded
    private BufferedImage categoryImage; // JPG, PNG
}
