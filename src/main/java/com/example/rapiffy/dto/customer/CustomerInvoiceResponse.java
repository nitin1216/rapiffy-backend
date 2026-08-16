package com.example.rapiffy.dto.customer;

import com.example.rapiffy.dto.order.OrderItemResponse;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CustomerInvoiceResponse {

    private String orderNumber;
    private LocalDateTime orderDate;
    private String message;

    // Customer details
    private String customerPhone;
    private String deliveryAddress;
    private String deliveryType;

    // All shops in this order
    private List<ShopInvoiceSection> shops;

    // Grand totals
    private Double subtotal;
    private Double totalGst;
    private Double deliveryCharge;
    private Double totalAmount;

    @Data
    public static class ShopInvoiceSection {
        private String shopName;
        private String shopAddress;
        private String shopPhone;
        private Double shopTotal;
        private List<OrderItemResponse> items;
    }
}
