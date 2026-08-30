package com.example.rapiffy.dto.customer;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class CustomerVariantResponse {

    private Long variantId;
    private Long shopProductId;       // use this to add to cart
    private String variantName;
    private String brand;
    private String shortDescription;
    private Double mrp;
    private Double sellingPrice;
    private Integer stockQuantity;
    private String imageUrl;
    private List<String> imageUrls;

    // e.g. { "Size": "8", "Colour": "Red" }
    private Map<String, String> attributes;
}
