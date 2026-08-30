package com.example.rapiffy.services;

import com.example.rapiffy.dto.invoice.InvoiceResponse;
import com.example.rapiffy.dto.order.OrderDetailResponse;
import com.example.rapiffy.dto.order.OrderSummaryResponse;
import com.example.rapiffy.enums.OrderStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface AdminOrderService {

    List<OrderSummaryResponse> getOrders(Long userId, OrderStatus status);

    OrderDetailResponse getOrderDetail(Long userId, Long orderId);

    ResponseEntity<OrderDetailResponse> updateOrderStatus(Long userId, Long orderId, OrderStatus status);

    InvoiceResponse getInvoice(Long userId, Long orderId);
}
