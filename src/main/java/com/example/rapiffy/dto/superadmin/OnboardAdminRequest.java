package com.example.rapiffy.dto.superadmin;

import com.example.rapiffy.enums.CategoryType;
import lombok.Data;

import java.util.List;

/**
 * Request DTO for SuperAdmin to onboard a new Admin (shopkeeper).
 * SuperAdmin selects categories by enum — no need to know DB IDs.
 */
@Data
public class OnboardAdminRequest {

    // Auth credentials for the new Admin
    private String phoneNumber;
    private String password;
    private String email;

    // Shop details
    private String shopName;
    private List<CategoryType> categoryTypes;  // SUPERADMIN picks from enum
    private Double servingRangeInKm;
    private String gstNumber;

    // If true → all active MasterProducts for assigned categories are bulk-added as ShopProducts
    // If false → Admin activates products manually from catalog
    private boolean addAllProducts = false;

    // Bank details (only SuperAdmin can set/update)
    private String nameOnCard;
    private String merchantType;
    private String bankAccountNumber;
    private String ifsc;
}
