package com.example.rapiffy.dto.customer;

import lombok.Data;

@Data
public class CustomerAddressResponse {

    private Long addressId;
    private String label;
    private String addressLine1;
    private String city;
    private String state;
    private String pinCode;
    private String country;
    private String latitude;
    private String longitude;
    private boolean isDefault;
}
