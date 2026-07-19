package com.example.rapiffy.dto.order;

import com.example.rapiffy.enums.OrderStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDetailResponse {

    private Long orderId;
    private String orderNumber;
    private String invoiceId;

    // Customer info
    private String customerPhone;
    private String customerName;

    // Shop info
    private String shopName;

    // Items
    private List<OrderItemResponse> items;

    // Pricing
    private Double subtotal;
    private Double totalGst;
    private Double deliveryCharge;
    private Double totalAmount;

    // Delivery
    private String deliveryType;
    private String deliveryAddress;

    // Status
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
