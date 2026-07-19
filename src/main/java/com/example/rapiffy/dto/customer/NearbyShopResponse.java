package com.example.rapiffy.dto.customer;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class NearbyShopResponse {

    private Long shopId;
    private String shopName;
    private String city;
    private String addressLine1;
    private Double distanceInKm;
    private Double servingRangeInKm;
    private List<String> categories; // e.g. ["Grocery", "Dairy"]
}
