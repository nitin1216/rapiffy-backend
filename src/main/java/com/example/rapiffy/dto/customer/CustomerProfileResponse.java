package com.example.rapiffy.dto.customer;

import lombok.Data;

@Data
public class CustomerProfileResponse {

    private Long userId;
    private String email;
    private String phoneNumber;
    private String prefix;
    private String firstName;
    private String middleName;
    private String lastName;
    private String suffix;
}
