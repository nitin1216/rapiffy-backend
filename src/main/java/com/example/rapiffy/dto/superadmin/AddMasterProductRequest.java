package com.example.rapiffy.dto.superadmin;

import lombok.Data;

import java.util.List;

@Data
public class AddMasterProductRequest {

    private Long categoryId;
    private String productCode;
    private String productName;
    private String brand;
    private String unit;
    private String unitValue;
    private Double mrp;
    private String imageUrl;
    private String shortDescription;
    private String longDescription;
    private boolean hasVariants = false;
    private List<MasterVariantRequest> variants;
}
