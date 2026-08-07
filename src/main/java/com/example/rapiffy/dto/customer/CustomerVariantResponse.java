package com.example.rapiffy.dto.customer;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerVariantResponse {

    private Long variantId;
    private Long shopProductId;       // use this to add to cart
    private String variantName;
    private String brand;
    private String unit;
    private String unitValue;
    private String shortDescription;
    private Double mrp;
    private Double sellingPrice;
    private Integer stockQuantity;
    private String imageUrl;
}
