package com.example.rapiffy.dto;

import lombok.Data;

@Data
public class GoogleAuthRequest {

    // The ID token received from Google Sign-In on the frontend
    private String idToken;
}
