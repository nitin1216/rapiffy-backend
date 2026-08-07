package com.example.rapiffy.dto.platform;

import lombok.Data;

@Data
public class SetCommissionRateRequest {
    private Long categoryId;
    private Double commissionRate;      // customer-side %
    private Double shopCommissionRate;  // shop-side %
    private String notes;
}
