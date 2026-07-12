package com.example.rapiffy.dto.catalog;

import lombok.Data;

import java.time.LocalDate;

/**
 * DTO for a single variant inside a product request.
 *
 * - Has "id" → update existing variant
 * - No "id" (null) → add new variant
 * - Missing from list entirely → variant gets removed
 */
@Data
public class VariantRequest {

    // Null = new variant, Non-null = update existing
    private Long id;

    private String variantName;
    private String brand;
    private String unit;
    private String unitValue;
    private Double mrp;
    private Double sellingPrice;
    private Integer stockQuantity;
    private Integer thresholdQuantity;
    private String imageUrl;
    private LocalDate expiryDate;
}
