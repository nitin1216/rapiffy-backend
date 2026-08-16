package com.example.rapiffy.dto.catalog;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO when Admin updates an existing ShopProduct.
 * Only non-null fields will be updated.
 *
 * For variants:
 * - Has "id" → update existing variant
 * - No "id" → add new variant
 * - Missing from list → variant gets removed
 */
@Data
public class UpdateProductRequest {

    private String productName;
    private String shortDescription;
    private String longDescription;
    private String brand;
    private String imageUrl;
    private Double mrp;
    private Double sellingPrice;
    private Integer stockQuantity;
    private Integer thresholdQuantity;
    private String unit;
    private String unitValue;
    private LocalDate expiryDate;

    // Variants — null means don't touch variants, empty list means remove all
    private Boolean hasVariants;

    // e.g. ["Size", "Colour"] — replaces existing attribute types when provided
    private List<String> attributeTypes;
    private List<VariantRequest> variants;
}
