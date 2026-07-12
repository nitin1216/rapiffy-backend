package com.example.rapiffy.dto.catalog;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO when Admin activates a product from the master catalog.
 * Admin provides their own price, stock, and optional overrides.
 */
@Data
public class ActivateProductRequest {

    // Which MasterProduct to activate
    private Long masterProductId;

    // Admin's selling price (required if hasVariants=false)
    private Double sellingPrice;

    // How many units in stock (required if hasVariants=false)
    private Integer stockQuantity;

    // Alert when stock falls below this (optional)
    private Integer thresholdQuantity;

    // Optional overrides — if null, defaults from MasterProduct are used
    private String productName;
    private String shortDescription;
    private String longDescription;
    private String brand;
    private String unit;
    private String unitValue;
    private Double mrp;
    private String imageUrl;
    private LocalDate expiryDate;

    // Variants — default false (single product)
    private boolean hasVariants = false;
    private List<VariantRequest> variants;
}
