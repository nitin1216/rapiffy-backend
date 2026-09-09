package com.example.rapiffy.dto.delivery;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PushLocationRequest {

    @NotNull(message = "orderId is required")
    private Long orderId;

    @NotNull(message = "latitude is required")
    private Double latitude;

    @NotNull(message = "longitude is required")
    private Double longitude;

    // Optional
    private Double accuracy;
    private Integer batteryLevel;
}
