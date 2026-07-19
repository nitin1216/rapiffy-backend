package com.example.rapiffy.dto.superadmin;

import com.example.rapiffy.enums.CategoryType;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * SuperAdmin can update all Admin profile fields including bank details and categories.
 * All fields are optional — only non-null fields get updated.
 */
@Data
public class UpdateAdminProfileBySuperAdminRequest {

    // Personal details
    private String prefix;
    private String firstName;
    private String middleName;
    private String lastName;
    private String suffix;
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
    private List<CategoryType> categoryTypes;  // SUPERADMIN picks from enum
    private Double servingRangeInKm;
    private String gstNumber;
    private Integer noOfDeliveryPersons;

    // Only SUPERADMIN can toggle this flag
    private Boolean editUnlistedProducts;

    // Bank details (only SuperAdmin can update)
    private String nameOnCard;
    private String merchantType;
    private String bankAccountNumber;
    private String ifsc;
}
