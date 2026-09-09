package com.example.rapiffy.dto.delivery;

import com.example.rapiffy.dto.order.OrderItemResponse;
import com.example.rapiffy.enums.OrderStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DeliveryOrderDetailResponse {
    private Long orderId;
    private String orderNumber;

    // Shop info
    private String shopName;
    private String shopAddress;
    private String shopPhone;
    private String shopLatitude;
    private String shopLongitude;

    // Customer info
    private String customerPhone;
    private String deliveryAddress;
    private String deliveryLatitude;
    private String deliveryLongitude;

    // Items
    private List<OrderItemResponse> items;

    // Pricing
    private Double totalAmount;
    private Double deliveryCharge;

    // Status
    private OrderStatus status;
    private LocalDateTime assignedAt;
    private LocalDateTime createdAt;
}
