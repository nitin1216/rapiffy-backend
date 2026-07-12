package com.example.rapiffy.dto.superadmin;

import lombok.Data;

import java.util.List;

/**
 * Request DTO for SuperAdmin to onboard a new Admin (shopkeeper).
 * SuperAdmin provides shop details and assigns categories.
 */
@Data
public class OnboardAdminRequest {

    // Auth credentials for the new Admin
    private String phoneNumber;
    private String password;
    private String email;

    // Shop details
    private String shopName;
    private List<Long> categoryIds;  // categories this shop will sell
    private Double servingRangeInKm;
    private String gstNumber;

    // Bank details (only SuperAdmin can set/update)
    private String nameOnCard;
    private String merchantType;      // retail, eCommerce, wholesale
    private String bankAccountNumber;
    private String ifsc;
}
