package com.example.rapiffy.dto.customer;

import com.example.rapiffy.enums.OrderStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CustomerOrderSummaryResponse {

    private Long orderId;
    private String orderNumber;
    private String shopName;
    private Integer totalItems;
    private Double subtotal;
    private Double totalGst;
    private Double deliveryCharge;
    private Double totalAmount;
    private String deliveryType;
    private OrderStatus status;
    private LocalDateTime createdAt;
}
