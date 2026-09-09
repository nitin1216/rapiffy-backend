package com.example.rapiffy.dto.delivery;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeliveryRouteResponse {

    // Waypoint 1: Shop (pickup)
    private String shopName;
    private String shopLatitude;
    private String shopLongitude;
    private String shopAddress;

    // Waypoint 2: Customer (drop)
    private String customerLatitude;
    private String customerLongitude;
    private String customerDeliveryAddress;
}
