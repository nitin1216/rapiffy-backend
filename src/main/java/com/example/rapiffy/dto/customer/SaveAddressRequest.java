package com.example.rapiffy.dto.customer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SaveAddressRequest {

    // Label — e.g. "Home", "Office", "Other"
    @NotBlank(message = "Label is required")
    private String label;

    @NotBlank(message = "Address line is required")
    private String addressLine1;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Pin code is required")
    private String pinCode;

    private String country;
    private String latitude;
    private String longitude;

    // If true → this becomes the default address
    private boolean isDefault = false;
}
