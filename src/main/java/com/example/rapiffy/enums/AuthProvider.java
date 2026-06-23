package com.example.rapiffy.enums;

public enum AuthProvider {
    FACEBOOK("Facebook"),
    GOOGLE("Google"),
    NORMAL("Number or email");

    private final String message;

    AuthProvider(String message) {
        this.message = message;
    }
}
