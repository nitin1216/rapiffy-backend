package com.example.rapiffy.dto.delivery;

import com.example.rapiffy.enums.OrderStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeliveryOrderSummaryResponse {
    private Long orderId;
    private String orderNumber;
    private String shopName;
    private String customerPhone;
    private String deliveryAddress;
    private Double totalAmount;
    private Double deliveryCharge;
    private Integer totalItems;
    private OrderStatus status;
    private LocalDateTime assignedAt;
    private LocalDateTime createdAt;
}
