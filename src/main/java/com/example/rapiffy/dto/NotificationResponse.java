package com.example.rapiffy.dto;

import com.example.rapiffy.enums.OrderStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private Long orderId;
    private String orderNumber;
    private OrderStatus orderStatus;
    private boolean isRead;
    private LocalDateTime createdAt;
}
