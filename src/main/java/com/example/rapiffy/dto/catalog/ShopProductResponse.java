package com.example.rapiffy.dto.catalog;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ShopProductResponse {

    private Long shopProductId;
    private Long masterProductId;
    private String productName;
    private String brand;
    private String unit;
    private String unitValue;
    private Double mrp;
    private Double sellingPrice;
    private Integer stockQuantity;
    private Integer thresholdQuantity;
    private String imageUrl;
    private String shortDescription;
    private LocalDate expiryDate;
    private boolean hasVariants;
    private boolean isActive;
    private boolean unlisted;

    // e.g. ["Size", "Colour"] — only when hasVariants = true
    private List<String> attributeTypes;
    private List<VariantResponse> variants;
}
