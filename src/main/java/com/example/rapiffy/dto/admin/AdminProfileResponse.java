package com.example.rapiffy.dto.admin;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for Admin's profile view.
 * Bank details are masked (only last 4 digits visible).
 */
@Data
public class AdminProfileResponse {

    private Long profileId;

    // Personal
    private String prefix;
    private String firstName;
    private String middleName;
    private String lastName;
    private String suffix;

    private String email;
    private String phoneNumber; // read-only (from User)
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

    // Shop
    private String shopName;
    private List<String> shopCategories;
    private Double servingRangeInKm;
    private String gstNumber;
    private Integer noOfDeliveryPersons;

    // Bank (masked)
    private String nameOnCard;
    private String merchantType;
    private String maskedAccountNumber; // e.g. "xxxxxxxx1234"
    private String maskedIfsc;          // e.g. "xxxx123"

    // Subscription (read-only)
    private LocalDate subscriptionStartDate;
    private LocalDate subscriptionEndDate;
    private String subscriptionStatus;
}
