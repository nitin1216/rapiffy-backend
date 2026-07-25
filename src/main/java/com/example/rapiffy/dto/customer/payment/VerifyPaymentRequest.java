package com.example.rapiffy.dto.customer.payment;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request sent by frontend after customer completes payment on Razorpay checkout.
 * Contains all 3 values returned by Razorpay to verify authenticity.
 */
@Data
public class VerifyPaymentRequest {

    @NotBlank(message = "razorpayOrderId is required")
    private String razorpayOrderId;

    @NotBlank(message = "razorpayPaymentId is required")
    private String razorpayPaymentId;

    @NotBlank(message = "razorpaySignature is required")
    private String razorpaySignature;
}
