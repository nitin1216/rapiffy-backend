package com.example.rapiffy.dto.customer;

import lombok.Data;

@Data
public class CartItemResponse {

    private Long cartItemId;
    private Long shopProductId;
    private String productName;
    private String brand;
    private String unit;
    private String unitValue;
    private String imageUrl;
    private Double mrp;
    private Double sellingPrice;
    private Integer quantity;
    private Double itemTotal; // sellingPrice * quantity
}
