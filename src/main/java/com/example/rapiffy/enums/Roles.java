package com.example.rapiffy.enums;

public enum Roles {
    ADMIN("ShopKeeper"),
    CUSTOMER("Customer"),
    DELIVERY("Delivery Person");

    private final String message;

    Roles(String message) {
        this.message = message;
    }
}
