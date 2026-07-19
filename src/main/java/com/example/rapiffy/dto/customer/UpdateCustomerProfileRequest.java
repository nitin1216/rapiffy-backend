package com.example.rapiffy.dto.customer;

import lombok.Data;

@Data
public class UpdateCustomerProfileRequest {

    private String prefix;
    private String firstName;
    private String middleName;
    private String lastName;
    private String suffix;
    private String email;
}
