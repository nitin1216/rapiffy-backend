package com.example.rapiffy.dto.delivery;

import com.example.rapiffy.enums.OrderStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AssignDeliveryResponse {
    private Long orderId;
    private String orderNumber;
    private OrderStatus status;
    private Long deliveryPersonUserId;
    private String deliveryPersonName;
    private String deliveryPersonPhone;
    private LocalDateTime assignedAt;
}
