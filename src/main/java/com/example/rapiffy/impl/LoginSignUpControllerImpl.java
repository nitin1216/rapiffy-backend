package com.example.rapiffy.impl;

import com.example.rapiffy.controller.LoginSignUpController;
import com.example.rapiffy.dto.LoginRequest;
import com.example.rapiffy.dto.LoginResponse;
import com.example.rapiffy.dto.SignUpRequest;
import com.example.rapiffy.dto.SignUpResponse;
import com.example.rapiffy.enums.Roles;
import com.example.rapiffy.services.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginSignUpControllerImpl implements LoginSignUpController {

    private final AuthService authService;

    public LoginSignUpControllerImpl(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public ResponseEntity<LoginResponse> login(LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<SignUpResponse> signUpAdmin(SignUpRequest request) {
        // Force role to ADMIN regardless of what was sent in body
        request.setRole(Roles.ADMIN);
        SignUpResponse response = authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<SignUpResponse> signUpCustomer(SignUpRequest request) {
        // Force role to CUSTOMER
        request.setRole(Roles.CUSTOMER);
        SignUpResponse response = authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<SignUpResponse> signUpDelivery(SignUpRequest request) {
        // Force role to DELIVERY
        request.setRole(Roles.DELIVERY);
        SignUpResponse response = authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
