package com.example.rapiffy.dto.delivery;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeliveryOrderStatsResponse {
    private long active;
    private long delivered;
    private long total;
    private Double totalEarned;
}
