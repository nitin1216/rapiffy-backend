package com.example.rapiffy.dto.admin;

import com.example.rapiffy.enums.DeliveryBoyOnboardedBy;
import lombok.Data;

@Data
public class DeliveryPersonResponse {
    private Long deliveryPersonId;
    private Long userId;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private DeliveryBoyOnboardedBy onboardedBy;
    private boolean active;
}
