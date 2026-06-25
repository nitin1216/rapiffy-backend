package com.example.rapiffy.services;

import com.example.rapiffy.dto.GoogleAuthRequest;
import com.example.rapiffy.dto.LoginResponse;

public interface GoogleAuthService {

    /**
     * Verifies the Google ID token, finds or creates the user,
     * and returns a JWT for the app.
     */
    LoginResponse googleLogin(GoogleAuthRequest request);
}
