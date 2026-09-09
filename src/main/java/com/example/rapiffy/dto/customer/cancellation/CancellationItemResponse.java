package com.example.rapiffy.dto.customer.cancellation;

import lombok.Data;

@Data
public class CancellationItemResponse {
    private Long orderItemId;
    private String productName;
    private String imageUrl;
    private Integer quantityToCancel;
    private Double lineRefundAmount;
}
