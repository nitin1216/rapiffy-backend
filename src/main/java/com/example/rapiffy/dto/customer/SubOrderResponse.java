package com.example.rapiffy.dto.customer;

import com.example.rapiffy.dto.order.OrderItemResponse;
import com.example.rapiffy.enums.OrderStatus;
import lombok.Data;

import java.util.List;

@Data
public class SubOrderResponse {

    private Long subOrderId;
    private String subOrderNumber;
    private String shopName;
    private Double subtotal;
    private Double totalGst;
    private Double totalAmount;
    private OrderStatus status;
    private List<OrderItemResponse> items;
}
