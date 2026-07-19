package com.example.rapiffy.dto.order;

import lombok.Data;

@Data
public class OrderItemResponse {

    private Long orderItemId;
    private Long shopProductId;
    private String productName;
    private String brand;
    private String unit;
    private String unitValue;
    private String imageUrl;
    private Double mrp;
    private Double sellingPrice;
    private Integer quantity;
    private String gstSlab;
    private Double gstAmount;
    private Double lineTotal;
}
