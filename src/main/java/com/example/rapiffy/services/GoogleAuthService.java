package com.example.rapiffy.services;

import com.example.rapiffy.dto.GoogleAuthRequest;
import com.example.rapiffy.dto.LoginResponse;
import com.example.rapiffy.enums.Roles;

public interface GoogleAuthService {

    /**
     * Verifies the Google ID token, finds or creates the user with the given role,
     * and returns a JWT for the app.
     * - New user → created with the provided role
     * - Existing user → role is NOT overwritten (they keep their existing role)
     */
    LoginResponse googleLogin(GoogleAuthRequest request, Roles role);
}
