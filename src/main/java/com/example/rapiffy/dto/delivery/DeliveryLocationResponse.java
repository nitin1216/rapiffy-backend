package com.example.rapiffy.dto.delivery;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeliveryLocationResponse {

    private Long orderId;
    private Double latitude;
    private Double longitude;
    private Double accuracy;
    private Integer batteryLevel;
    private LocalDateTime recordedAt;

    // How many seconds ago the location was last updated
    // Customer app can use this to show "Last updated X sec ago"
    private Long secondsSinceUpdate;
}
