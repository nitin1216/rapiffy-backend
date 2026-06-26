package com.example.rapiffy.enums;

public enum CategoryStatus {

    FASHION("Fashion"),
    ELECTRONICS("Electronics"),
    KITCHEN("Kitchen"),
    FOOD("Food"),
    GROCERY("Grocery"),
    MEDICAL("Medical"),
    BEAUTY("Beauty"),
    TOYS("Toys"),
    SPORTS("Sports");

    private final String displayName;

    CategoryStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
