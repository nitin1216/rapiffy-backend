package com.example.rapiffy.controller;

import com.example.rapiffy.dto.GoogleAuthRequest;
import com.example.rapiffy.dto.LoginRequest;
import com.example.rapiffy.dto.LoginResponse;
import com.example.rapiffy.dto.SignUpRequest;
import com.example.rapiffy.dto.SignUpResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Auth", description = "Signup and Login APIs")
@RequestMapping("v1/auth")
public interface LoginSignUpController {

    // ── Login (all roles use same endpoint) ───────────────────────────────────

    @Operation(summary = "Login with phone number and password")
    @PostMapping("/login")
    ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request);

    // ── Signup ────────────────────────────────────────────────────────────────

    @Operation(summary = "Super Admin signup")
    @PostMapping("/super-admin-sign-up")
    ResponseEntity<SignUpResponse> signUpSuperAdmin(@RequestBody SignUpRequest request);

    @Operation(summary = "Customer signup")
    @PostMapping("/customer-sign-up")
    ResponseEntity<SignUpResponse> signUpCustomer(@RequestBody SignUpRequest request);

    @Operation(summary = "Delivery person signup")
    @PostMapping("/deli-sign-up")
    ResponseEntity<SignUpResponse> signUpDelivery(@RequestBody SignUpRequest request);

    // ── Google OAuth ──────────────────────────────────────────────────────────
    // Note: Admin cannot self-signup. Only SuperAdmin onboards Admin.

    @Operation(summary = "Customer — login or register with Google")
    @PostMapping("/google/customer")
    ResponseEntity<LoginResponse> googleLoginCustomer(@RequestBody GoogleAuthRequest request);

    @Operation(summary = "Delivery person — login or register with Google")
    @PostMapping("/google/delivery")
    ResponseEntity<LoginResponse> googleLoginDelivery(@RequestBody GoogleAuthRequest request);
}
