package com.example.rapiffy.dto.superadmin;

import lombok.Data;

/**
 * DTO for a single variant in MasterProduct.
 * Used when SuperAdmin adds/updates variants on a catalog product.
 *
 * - Has "id" → update existing variant
 * - No "id" → add new variant
 * - Missing from list → variant gets removed
 */
@Data
public class MasterVariantRequest {

    private Long id;
    private String variantName;
    private String brand;
    private String unit;
    private String unitValue;
    private Double mrp;
    private String imageUrl;
}
