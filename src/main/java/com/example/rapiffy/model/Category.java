package com.example.rapiffy.model;

import com.example.rapiffy.enums.CategoryStatus;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "categories")
@Data
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_code", unique = true, nullable = false)
    private String categoryCode;

    @Column(name = "category_name", nullable = false)
    private String categoryName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_type", nullable = false)
    private CategoryStatus categoryType;

    // Store image as binary (JPG/PNG). Use @Lob for large binary data.
    @Lob
    @Column(name = "category_image")
    private byte[] categoryImage;
}
