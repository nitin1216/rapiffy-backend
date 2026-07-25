package com.example.rapiffy.dto.customer.payment;

import lombok.Data;

/**
 * Response returned to frontend after initiating payment.
 * Frontend uses these values to open Razorpay checkout popup.
 */
@Data
public class InitiatePaymentResponse {

    // Razorpay order ID — frontend passes this to Razorpay checkout
    private String razorpayOrderId;

    // Amount in paise (₹850 = 85000)
    private Long amount;

    // Currency (always "INR")
    private String currency;

    // Razorpay public key — frontend needs this to initialize checkout
    private String razorpayKeyId;

    // Our parent order number (for display on checkout)
    private String orderNumber;
}
