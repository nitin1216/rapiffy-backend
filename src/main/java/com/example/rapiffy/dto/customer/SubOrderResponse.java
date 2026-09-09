package com.example.rapiffy.dto.customer;

import com.example.rapiffy.dto.order.OrderItemResponse;
import com.example.rapiffy.enums.DeliveryType;
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
    private Double deliveryCharge;
    private Double totalAmount;
    private DeliveryType deliveryType;
    private OrderStatus status;
    private List<OrderItemResponse> items;
}
