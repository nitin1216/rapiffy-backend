package com.example.rapiffy.dto.customer.payment;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request to cancel a specific sub-order (one shop's order).
 * Customer provides reason for cancellation.
 */
@Data
public class CancelSubOrderRequest {

    @NotBlank(message = "Cancellation reason is required")
    private String reason;
}
