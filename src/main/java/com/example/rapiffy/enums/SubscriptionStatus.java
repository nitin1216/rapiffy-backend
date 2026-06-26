package com.example.rapiffy.enums;

public enum SubscriptionStatus {
    ACTIVE("Active"),
    EXPIRED("Expired");

    private String displayName;
    SubscriptionStatus(String message){
        this.displayName = message;
    }

    public String getDisplayName() {
        return displayName;
    }
}
