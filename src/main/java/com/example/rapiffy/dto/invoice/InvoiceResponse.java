package com.example.rapiffy.dto.invoice;

import com.example.rapiffy.dto.order.OrderItemResponse;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class InvoiceResponse {

    // Invoice meta
    private String invoiceId;
    private String orderNumber;
    private LocalDateTime invoiceDate;

    // Sold by (Shop / Admin)
    private String shopName;
    private String shopAddress;
    private String shopGstNumber;
    private String shopPhone;

    // Billed to (Customer)
    private String customerName;
    private String customerPhone;
    private String deliveryAddress;

    // Items
    private List<OrderItemResponse> items;

    // Totals
    private Double subtotal;
    private Double totalGst;
    private Double deliveryCharge;
    private Double totalAmount;

    // Delivery
    private String deliveryType;
}
