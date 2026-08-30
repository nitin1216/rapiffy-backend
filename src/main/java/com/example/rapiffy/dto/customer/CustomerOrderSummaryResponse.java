package com.example.rapiffy.dto.customer;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CustomerOrderSummaryResponse {

    private Long orderId;
    private String orderNumber;
    private Integer totalItems;
    private String thumbnailImage;
    private Double subtotal;
    private Double totalGst;
    private Double deliveryCharge;
    private Double totalAmount;
    private String deliveryType;
    private LocalDateTime createdAt;
}
