package com.example.rapiffy.common;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class CAddress {

    private String pinCode;
    private String state;
    private String city;
    private String country;
    private String addressLine1;
    private String latitude;
    private String longitude;
}
