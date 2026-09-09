package com.example.rapiffy.dto.delivery;

import lombok.Data;

@Data
public class OnboardDeliveryPersonRequest {
    private String phoneNumber;
    private String password;
    private String firstName;
    private String lastName;
}
