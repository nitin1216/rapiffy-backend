package com.example.rapiffy.dto.customer;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
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
    private String categoryName;
    private Long categoryId;
    private boolean hasVariants;
    private List<CustomerVariantResponse> variants;
    private Long shopId;
    private String shopName;
    private Double distanceInKm;
}
