package com.example.rapiffy.dto.customer;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerProductResponse {

    private Long shopProductId;
    private String productName;
    private String brand;
    private String unit;
    private String unitValue;
    private Double mrp;
    private Double sellingPrice;
    private Integer stockQuantity;
    private String imageUrl;
    private String shortDescription;
    private Long subCategoryId;
    private String subCategoryName;
    private Long categoryId;
    private String categoryName;
    private boolean hasVariants;

    // e.g. ["Size", "Colour"] — only when hasVariants = true
    private List<String> attributeTypes;
    private List<CustomerVariantResponse> variants;
    private Long shopId;
    private String shopName;
    private Double distanceInKm;
}
