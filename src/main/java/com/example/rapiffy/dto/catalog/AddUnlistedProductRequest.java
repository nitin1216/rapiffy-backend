package com.example.rapiffy.dto.catalog;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO when Admin adds a custom/unlisted product (not in master catalog).
 * All fields must be provided by Admin since there's no MasterProduct to inherit from.
 */
@Data
public class AddUnlistedProductRequest {

    // Which subCategory this product belongs to (must be under one of Admin's shopCategories)
    private Long subCategoryId;

    // Required fields
    private String productName;
    private Double sellingPrice;
    private Integer stockQuantity;

    // Optional fields
    private String shortDescription;
    private String longDescription;
    private String brand;
    private String imageUrl;
    private Double mrp;
    private Integer thresholdQuantity;
    private String unit;
    private String unitValue;
    private LocalDate expiryDate;

    // Variants — default false (single product)
    private boolean hasVariants = false;
    private List<VariantRequest> variants;
}
