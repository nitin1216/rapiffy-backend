package com.example.rapiffy.dto.catalog;

import lombok.Data;

import java.time.LocalDate;

/**
 * Response DTO for a single product variant.
 */
@Data
public class VariantResponse {

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
    private boolean isActive;
}
