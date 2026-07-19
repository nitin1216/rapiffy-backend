package com.example.rapiffy.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum representing all supported shop categories on the platform.
 * SUPERADMIN assigns one or more of these to an ADMIN during onboarding.
 *
 * CLOTH is special — Admins with this category can add unlisted products (editUnlistedProducts = true).
 */
public enum CategoryType {
    GROCERY("Grocery"),
    DAIRY("Dairy"),
    CLOTH("Cloth"),
    MEDICAL("Medical"),
    ELECTRONICS("Electronics"),
    BAKERY("Bakery"),
    PHARMACY("Pharmacy"),
    STATIONERY("Stationery"),
    HARDWARE("Hardware"),
    PERSONAL_CARE("Personal Care");

    private final String displayName;

    CategoryType(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String display() {
        return displayName;
    }

    @JsonCreator
    public static CategoryType fromValue(String value) {
        for (CategoryType type : values()) {
            if (type.name().equalsIgnoreCase(value) || type.displayName.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown CategoryType: " + value);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
