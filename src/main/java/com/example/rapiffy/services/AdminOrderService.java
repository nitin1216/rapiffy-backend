package com.example.rapiffy.services;

import com.example.rapiffy.dto.invoice.InvoiceResponse;
import com.example.rapiffy.dto.order.OrderDetailResponse;
import com.example.rapiffy.dto.order.OrderSummaryResponse;
import com.example.rapiffy.enums.OrderStatus;

import java.util.List;

public interface AdminOrderService {

    List<OrderSummaryResponse> getOrders(Long userId, OrderStatus status);

    OrderDetailResponse getOrderDetail(Long userId, Long orderId);

    OrderDetailResponse confirmOrder(Long userId, Long orderId);

    OrderDetailResponse markReady(Long userId, Long orderId);

    OrderDetailResponse markOutForDelivery(Long userId, Long orderId);

    OrderDetailResponse markDelivered(Long userId, Long orderId);

    InvoiceResponse getInvoice(Long userId, Long orderId);
}
