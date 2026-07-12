package com.example.rapiffy.dto.catalog;

import lombok.Data;

/**
 * Response DTO shown to Admin when browsing the catalog.
 * Combines MasterProduct info + whether Admin has activated it in their shop.
 */
@Data
public class CatalogProductResponse {

    // From MasterProduct
    private Long masterProductId;
    private String productCode;
    private String productName;
    private String brand;
    private String unit;
    private String unitValue;
    private Double mrp;
    private String imageUrl;
    private String shortDescription;
    private String categoryName;

    // Shop-specific (null if Admin hasn't activated this product)
    private Long shopProductId;
    private boolean activatedInShop;
    private Double sellingPrice;
    private Integer stockQuantity;
}
