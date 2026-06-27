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

    // ── Normal (phone + password) ─────────────────────────────────────────────

    @Operation(summary = "Login with phone number and password")
    @PostMapping("/login")
    ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request);

    @Operation(summary = "Admin signup with phone number and password")
    @PostMapping("/admin-sign-up")
    ResponseEntity<SignUpResponse> signUpAdmin(@RequestBody SignUpRequest request);

    @Operation(summary = "Customer signup with phone number and password")
    @PostMapping("/customer-sign-up")
    ResponseEntity<SignUpResponse> signUpCustomer(@RequestBody SignUpRequest request);

    @Operation(summary = "Delivery person signup with phone number and password")
    @PostMapping("/deli-sign-up")
    ResponseEntity<SignUpResponse> signUpDelivery(@RequestBody SignUpRequest request);

    // ── Google OAuth ──────────────────────────────────────────────────────────

    @Operation(
        summary = "Admin — login or register with Google",
        description = "New Google user → created as ADMIN. Existing user → logs in with their current role."
    )
    @PostMapping("/google/admin")
    ResponseEntity<LoginResponse> googleLoginAdmin(@RequestBody GoogleAuthRequest request);

    @Operation(
        summary = "Customer — login or register with Google",
        description = "New Google user → created as CUSTOMER. Existing user → logs in with their current role."
    )
    @PostMapping("/google/customer")
    ResponseEntity<LoginResponse> googleLoginCustomer(@RequestBody GoogleAuthRequest request);

    @Operation(
        summary = "Delivery person — login or register with Google",
        description = "New Google user → created as DELIVERY. Existing user → logs in with their current role."
    )
    @PostMapping("/google/delivery")
    ResponseEntity<LoginResponse> googleLoginDelivery(@RequestBody GoogleAuthRequest request);
}
