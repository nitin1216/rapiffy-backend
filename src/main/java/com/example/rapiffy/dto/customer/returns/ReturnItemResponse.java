package com.example.rapiffy.dto.customer.returns;

import lombok.Data;

@Data
public class ReturnItemResponse {

    private Long orderItemId;
    private String productName;
    private String imageUrl;
    private Integer quantityToReturn;
    private Double sellingPrice;
    private Double lineRefundAmount;
}
