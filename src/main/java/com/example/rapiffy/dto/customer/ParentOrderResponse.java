package com.example.rapiffy.dto.customer;

import com.example.rapiffy.enums.OrderStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ParentOrderResponse {

    private Long parentOrderId;
    private String orderNumber;

    // Delivery info (shared across all sub-orders)
    private String deliveryType;
    private String deliveryAddress;
    private String deliveryInstruction;

    // Pricing (sum of all sub-orders)
    private Double subtotal;
    private Double totalGst;
    private Double totalAmount;

    // Overall status
    private OrderStatus status;

    // Sub-orders — one per shop
    private List<SubOrderResponse> subOrders;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
