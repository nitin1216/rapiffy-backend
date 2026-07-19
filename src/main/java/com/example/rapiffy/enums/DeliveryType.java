package com.example.rapiffy.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DeliveryType {
    SELF("self"),
    COURIER("Courier");

    private final String displayName;

    DeliveryType(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String display() {
        return displayName;
    }
}
