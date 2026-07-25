package com.example.rapiffy.dto.customer.payment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request to initiate payment for a placed order.
 * Frontend sends this after customer confirms the order and clicks "Pay".
 */
@Data
public class InitiatePaymentRequest {

    @NotNull(message = "parentOrderId is required")
    private Long parentOrderId;
}
