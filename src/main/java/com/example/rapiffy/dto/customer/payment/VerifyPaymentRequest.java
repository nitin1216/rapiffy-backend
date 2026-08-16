package com.example.rapiffy.dto.customer.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyPaymentRequest {

    @JsonProperty("razorpay_order_id")
    @NotBlank(message = "razorpayOrderId is required")
    private String razorpayOrderId;

    @JsonProperty("razorpay_payment_id")
    @NotBlank(message = "razorpayPaymentId is required")
    private String razorpayPaymentId;

    @JsonProperty("razorpay_signature")
    @NotBlank(message = "razorpaySignature is required")
    private String razorpaySignature;
}
