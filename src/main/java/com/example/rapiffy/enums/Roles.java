package com.example.rapiffy.enums;

public enum Roles {
    ADMIN("ShopKeeper"),
    CUSTOMER("Customer"),
    DELIVERY("Delivery Person");

    private final String displayName;

    Roles(String message) {
        this.displayName = message;
    }

    public String getDisplayName() {
        return displayName;
    }
}
