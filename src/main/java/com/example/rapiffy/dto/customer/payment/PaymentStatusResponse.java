package com.example.rapiffy.dto.customer.payment;

import com.example.rapiffy.enums.PaymentMethod;
import com.example.rapiffy.enums.PaymentStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response showing payment status for a parent order.
 * Customer can check if payment went through, is pending, or failed.
 */
@Data
public class PaymentStatusResponse {

    private Long parentOrderId;
    private String orderNumber;

    // Payment info
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private Double amount;
    private Double refundedAmount;
    private String currency;

    // Timestamps
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
