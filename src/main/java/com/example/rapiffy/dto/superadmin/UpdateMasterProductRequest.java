package com.example.rapiffy.dto.superadmin;

import lombok.Data;

import java.util.List;

/**
 * Request DTO for SuperAdmin to update a MasterProduct.
 * Includes variant management in the same request.
 */
@Data
public class UpdateMasterProductRequest {

    private String productName;
    private String brand;
    private String unit;
    private String unitValue;
    private Double mrp;
    private String shortDescription;
    private String longDescription;

    // Variants — null means don't touch, empty list means remove all
    private Boolean hasVariants;
    private List<String> attributeTypes;
    private List<MasterVariantRequest> variants;
}
