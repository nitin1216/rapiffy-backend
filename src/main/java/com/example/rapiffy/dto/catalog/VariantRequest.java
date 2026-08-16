package com.example.rapiffy.dto.catalog;

import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

@Data
public class VariantRequest {

    // Present when updating an existing variant; absent when adding a new one
    private Long id;

    private String variantName;
    private String brand;
    private String shortDescription;
    private String longDescription;
    private Double mrp;
    private Double sellingPrice;
    private Integer stockQuantity;
    private Integer thresholdQuantity;
    private String imageUrl;
    private LocalDate expiryDate;
    private String gstSlab;

    // e.g. { "Size": "8", "Colour": "Red" }
    private Map<String, String> attributes;
}
