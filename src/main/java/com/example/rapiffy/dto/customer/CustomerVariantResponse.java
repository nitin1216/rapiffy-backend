package com.example.rapiffy.dto.customer;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerVariantResponse {

    private Long variantId;
    private String variantName;
    private String brand;
    private String unit;
    private String unitValue;
    private Double mrp;
    private Double sellingPrice;
    private Integer stockQuantity;
    private String imageUrl;
}
