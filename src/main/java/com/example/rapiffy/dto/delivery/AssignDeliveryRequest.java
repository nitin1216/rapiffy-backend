package com.example.rapiffy.dto.delivery;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignDeliveryRequest {

    @NotNull(message = "deliveryPersonUserId is required")
    private Long deliveryPersonUserId;
}
