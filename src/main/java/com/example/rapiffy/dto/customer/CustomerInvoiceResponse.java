package com.example.rapiffy.dto.customer;

import com.example.rapiffy.dto.order.OrderItemResponse;
import com.example.rapiffy.enums.DeliveryType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CustomerInvoiceResponse {

    private String orderNumber;
    private String invoiceNumber;
    private LocalDateTime orderDate;
    private LocalDateTime invoiceDate;
    private String message;

    // Customer details
    private String customerPhone;
    private String deliveryAddress;
    private DeliveryType deliveryType;
    private String placeOfSupply;    // state from delivery address
    private String placeOfDelivery;  // same as placeOfSupply

    // Platform fee (page 2)
    private Double platformFee;      // base fee before GST
    private Double platformFeeGst;   // 18% GST on platform fee
    private Double platformFeeTotal; // platformFee + platformFeeGst
    private String txnId;            // Razorpay transfer ID

    // All shops in this order (always 1 for sub-order invoice)
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
        private String shopGstNumber;
        private String shopPan;
        private String shopState;
        private Double shopTotal;
        private List<OrderItemResponse> items;
    }
}
