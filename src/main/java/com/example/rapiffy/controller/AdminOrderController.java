package com.example.rapiffy.controller;

import com.example.rapiffy.dto.invoice.InvoiceResponse;
import com.example.rapiffy.dto.order.OrderDetailResponse;
import com.example.rapiffy.dto.order.OrderSummaryResponse;
import com.example.rapiffy.enums.OrderStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin Orders", description = "Shopkeeper order management APIs")
@RequestMapping("v1/admin/orders")
public interface AdminOrderController {

    @Operation(summary = "Get all orders", description = "Optionally filter by status e.g. ?status=PENDING")
    @GetMapping
    ResponseEntity<List<OrderSummaryResponse>> getOrders(
        @RequestParam(required = false) OrderStatus status
    );

    @Operation(summary = "Get order detail with all items")
    @GetMapping("/{orderId}")
    ResponseEntity<OrderDetailResponse> getOrderDetail(@PathVariable Long orderId);

    @Operation(summary = "Confirm order", description = "PENDING → CONFIRMED. Deducts stock.")
    @PutMapping("/{orderId}/confirm")
    ResponseEntity<OrderDetailResponse> confirmOrder(@PathVariable Long orderId);

    @Operation(summary = "Mark order as ready", description = "CONFIRMED → READY")
    @PutMapping("/{orderId}/ready")
    ResponseEntity<OrderDetailResponse> markReady(@PathVariable Long orderId);

    @Operation(summary = "Mark order out for delivery", description = "READY → OUT_FOR_DELIVERY")
    @PutMapping("/{orderId}/out-for-delivery")
    ResponseEntity<OrderDetailResponse> markOutForDelivery(@PathVariable Long orderId);

    @Operation(summary = "Mark order as delivered", description = "OUT_FOR_DELIVERY → DELIVERED")
    @PutMapping("/{orderId}/delivered")
    ResponseEntity<OrderDetailResponse> markDelivered(@PathVariable Long orderId);

    @Operation(summary = "Get invoice data as JSON")
    @GetMapping("/{orderId}/invoice")
    ResponseEntity<InvoiceResponse> getInvoice(@PathVariable Long orderId);

    @Operation(summary = "Download invoice as PDF")
    @GetMapping(value = "/{orderId}/invoice/pdf", produces = "application/pdf")
    ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long orderId);
}
