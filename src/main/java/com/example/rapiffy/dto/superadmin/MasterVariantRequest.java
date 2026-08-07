package com.example.rapiffy.dto.superadmin;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MasterVariantRequest {

    private String variantName;
    private String brand;
    private String unit;
    private String unitValue;
    private String shortDescription;
    private String longDescription;
    private Double mrp;
    private Double sellingPrice;
    private Integer stockQuantity;
    private Integer thresholdQuantity;
    private String imageUrl;
    private LocalDate expiryDate;
    private String gstSlab;
}
