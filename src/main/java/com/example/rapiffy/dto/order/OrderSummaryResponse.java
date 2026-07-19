package com.example.rapiffy.dto.order;

import com.example.rapiffy.enums.OrderStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderSummaryResponse {

    private Long orderId;
    private String orderNumber;
    private String customerPhone;
    private String customerName;
    private Double subtotal;
    private Double totalGst;
    private Double deliveryCharge;
    private Double totalAmount;
    private Integer totalItems;
    private OrderStatus status;
    private String deliveryType;
    private LocalDateTime createdAt;
}
