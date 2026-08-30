package com.example.rapiffy.dto.superadmin;

import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

@Data
public class MasterVariantRequest {

    private String variantName;
    private String brand;
    private String shortDescription;
    private String longDescription;
    private Double mrp;
    private Double sellingPrice;
    private Integer stockQuantity;
    private Integer thresholdQuantity;
    private LocalDate expiryDate;
    private String gstSlab;

    // e.g. { "Size": "8", "Colour": "Red" }
    private Map<String, String> attributes;
}
