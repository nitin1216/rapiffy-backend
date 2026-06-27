package com.example.rapiffy.impl;

import com.example.rapiffy.controller.LoginSignUpController;
import com.example.rapiffy.dto.GoogleAuthRequest;
import com.example.rapiffy.dto.LoginRequest;
import com.example.rapiffy.dto.LoginResponse;
import com.example.rapiffy.dto.SignUpRequest;
import com.example.rapiffy.dto.SignUpResponse;
import com.example.rapiffy.enums.Roles;
import com.example.rapiffy.services.AuthService;
import com.example.rapiffy.services.GoogleAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginSignUpControllerImpl implements LoginSignUpController {

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;

    public LoginSignUpControllerImpl(AuthService authService,
                                     GoogleAuthService googleAuthService) {
        this.authService = authService;
        this.googleAuthService = googleAuthService;
    }

    // ── Normal (phone + password) ─────────────────────────────────────────────

    @Override
    public ResponseEntity<LoginResponse> login(LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Override
    public ResponseEntity<SignUpResponse> signUpAdmin(SignUpRequest request) {
        request.setRole(Roles.ADMIN);
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signUp(request));
    }

    @Override
    public ResponseEntity<SignUpResponse> signUpCustomer(SignUpRequest request) {
        request.setRole(Roles.CUSTOMER);
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signUp(request));
    }

    @Override
    public ResponseEntity<SignUpResponse> signUpDelivery(SignUpRequest request) {
        request.setRole(Roles.DELIVERY);
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signUp(request));
    }

    // ── Google OAuth ──────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<LoginResponse> googleLoginAdmin(GoogleAuthRequest request) {
        return ResponseEntity.ok(googleAuthService.googleLogin(request, Roles.ADMIN));
    }

    @Override
    public ResponseEntity<LoginResponse> googleLoginCustomer(GoogleAuthRequest request) {
        return ResponseEntity.ok(googleAuthService.googleLogin(request, Roles.CUSTOMER));
    }

    @Override
    public ResponseEntity<LoginResponse> googleLoginDelivery(GoogleAuthRequest request) {
        return ResponseEntity.ok(googleAuthService.googleLogin(request, Roles.DELIVERY));
    }
}
