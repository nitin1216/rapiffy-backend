package com.example.rapiffy.enums;

public enum AuthProvider {
    FACEBOOK("Facebook"),
    GOOGLE("Google"),
    NORMAL("Number/E-mail");

    private final String displayName;

    AuthProvider(String message) {
        this.displayName = message;
    }

    public String getDisplayName() {
        return displayName;
    }
}
