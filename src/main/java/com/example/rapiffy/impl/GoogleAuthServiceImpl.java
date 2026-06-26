package com.example.rapiffy.impl;

import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.dto.GoogleAuthRequest;
import com.example.rapiffy.dto.LoginResponse;
import com.example.rapiffy.enums.AuthProvider;
import com.example.rapiffy.enums.Roles;
import com.example.rapiffy.model.User;
import com.example.rapiffy.model.profiles;
import com.example.rapiffy.repos.ProfileRepository;
import com.example.rapiffy.repos.UserRepository;
import com.example.rapiffy.security.JwtUtil;
import com.example.rapiffy.services.GoogleAuthService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class GoogleAuthServiceImpl implements GoogleAuthService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final JwtUtil jwtUtil;

    @Value("${google.client-id}")
    private String googleClientId;

    public GoogleAuthServiceImpl(UserRepository userRepository,
                                 ProfileRepository profileRepository,
                                 JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public LoginResponse googleLogin(GoogleAuthRequest request) {

        // 1. Verify the ID token with Google
        GoogleIdToken.Payload payload = verifyToken(request.getIdToken());

        // 2. Extract user info from the verified token
        String googleSub = payload.getSubject();         // unique Google user ID
        String email     = payload.getEmail();
        boolean verified = payload.getEmailVerified();
        String picture   = (String) payload.get("picture");
        String name      = (String) payload.get("name");

        if (!verified) {
            throw new ApiException("Google email is not verified", HttpStatus.UNAUTHORIZED);
        }

        // 3. Find existing user by googleSub, or fallback by email (e.g. was a phone user before)
        User user = userRepository.findByGoogleSub(googleSub)
                .or(() -> userRepository.findByEmail(email))
                .orElse(null);

        if (user == null) {
            // 4a. New user — create account automatically (role: CUSTOMER by default)
            user = new User();
            user.setEmail(email);
            user.setGoogleSub(googleSub);
            user.setEmailVerified(true);
            user.setPicture(picture);
            user.setAuthProvider(AuthProvider.GOOGLE);
            user.setRole(Roles.CUSTOMER);
            // No phone, no password for Google users

            User savedUser = userRepository.save(user);

            // Create empty profile linked to user
            profiles profile = new profiles();
            profile.setUser(savedUser);
            profileRepository.save(profile);

            user = savedUser;
        } else {
            // 4b. Existing user — update Google fields in case they changed
            user.setGoogleSub(googleSub);
            user.setEmailVerified(true);
            user.setPicture(picture);
            if (user.getAuthProvider() != AuthProvider.GOOGLE) {
                // Was a phone user who now signs in with Google on same email
                user.setAuthProvider(AuthProvider.GOOGLE);
            }
            userRepository.save(user);
        }

        // 5. Generate and return your app's JWT (same format as normal login)
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new LoginResponse(token, "Google login successful");
    }

    /**
     * Calls Google's servers to verify the token is genuine and not tampered with.
     * Throws ApiException if the token is invalid or expired.
     */
    private GoogleIdToken.Payload verifyToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier
                    .Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken == null) {
                throw new ApiException("Invalid Google ID token", HttpStatus.UNAUTHORIZED);
            }

            return idToken.getPayload();

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Failed to verify Google token: " + e.getMessage(),
                    HttpStatus.UNAUTHORIZED);
        }
    }
}
