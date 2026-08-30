package com.example.rapiffy.services.customer;

import com.example.rapiffy.dto.customer.CustomerInvoiceResponse;
import com.example.rapiffy.dto.customer.CustomerOrderSummaryResponse;
import com.example.rapiffy.dto.customer.ParentOrderResponse;
import com.example.rapiffy.dto.customer.PlaceOrderRequest;
import com.example.rapiffy.dto.invoice.InvoiceResponse;

import java.util.List;

public interface CustomerOrderService {

    ParentOrderResponse placeOrder(Long userId, PlaceOrderRequest request);

    List<CustomerOrderSummaryResponse> getMyOrders(Long userId);

    ParentOrderResponse getOrderDetail(Long userId, Long parentOrderId);

    CustomerInvoiceResponse getSubOrderInvoice(Long userId, Long parentOrderId, Long subOrderId);
}
