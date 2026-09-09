package com.example.rapiffy.services.customer;

import com.example.rapiffy.dto.customer.CancelOrderItemRequest;
import com.example.rapiffy.dto.customer.CancelOrderItemResponse;
import com.example.rapiffy.dto.customer.CustomerInvoiceResponse;
import com.example.rapiffy.dto.customer.CustomerOrderSummaryResponse;
import com.example.rapiffy.dto.customer.ParentOrderResponse;
import com.example.rapiffy.dto.customer.PlaceOrderRequest;
import com.example.rapiffy.dto.customer.cancellation.CancellationRequestResponse;
import com.example.rapiffy.dto.invoice.InvoiceResponse;
import com.example.rapiffy.dto.order.OrderItemResponse;

import java.util.List;

public interface CustomerOrderService {

    ParentOrderResponse placeOrder(Long userId, PlaceOrderRequest request);

    List<CustomerOrderSummaryResponse> getMyOrders(Long userId);

    ParentOrderResponse getOrderDetail(Long userId, Long parentOrderId);

    CustomerInvoiceResponse getSubOrderInvoice(Long userId, Long parentOrderId, Long subOrderId);

    List<OrderItemResponse> getSubOrderItems(Long userId, Long subOrderId);

    CancelOrderItemResponse cancelOrderItems(Long userId, Long subOrderId, CancelOrderItemRequest request);

    List<CancellationRequestResponse> getMyCancellations(Long userId);
}
