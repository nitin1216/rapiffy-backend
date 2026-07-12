package com.example.rapiffy.dto.admin;

import lombok.Data;

import java.time.LocalDate;

/**
 * Request DTO for Admin to update their own profile.
 * 
 * NOT editable by Admin:
 * - Phone number (used for login)
 * - Bank details (only SuperAdmin can update)
 * 
 * All fields are optional — only non-null fields get updated.
 */
@Data
public class UpdateAdminProfileRequest {

    // Personal details
    private String prefix;
    private String firstName;
    private String middleName;
    private String lastName;
    private String suffix;

    // Email (syncs to User table)
    private String email;

    private LocalDate dob;
    private String pan;
    private String aadhaar;

    // Address
    private String pinCode;
    private String state;
    private String city;
    private String country;
    private String addressLine1;
    private String latitude;
    private String longitude;

    // Shop details
    private String shopName;
    private Double servingRangeInKm;
    private String gstNumber;
    private Integer noOfDeliveryPersons;
}
