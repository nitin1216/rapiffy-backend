package com.example.rapiffy.dto.delivery;

import com.example.rapiffy.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateDeliveryStatusRequest {

    @NotNull(message = "status is required")
    private OrderStatus status;
}
