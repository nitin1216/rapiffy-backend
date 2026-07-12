package com.example.rapiffy.enums;

public enum Roles {
    ADMIN("ShopKeeper"),
    SUPER_ADMIN("Super Admin"),
    CUSTOMER("Customer"),
    DELIVERY("Delivery Person");

    private final String displayName;

    Roles(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
