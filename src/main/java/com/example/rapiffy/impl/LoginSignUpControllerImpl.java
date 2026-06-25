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

    @Override
    public ResponseEntity<LoginResponse> login(LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<LoginResponse> googleLogin(GoogleAuthRequest request) {
        LoginResponse response = googleAuthService.googleLogin(request);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<SignUpResponse> signUpAdmin(SignUpRequest request) {
        request.setRole(Roles.ADMIN);
        SignUpResponse response = authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<SignUpResponse> signUpCustomer(SignUpRequest request) {
        request.setRole(Roles.CUSTOMER);
        SignUpResponse response = authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<SignUpResponse> signUpDelivery(SignUpRequest request) {
        request.setRole(Roles.DELIVERY);
        SignUpResponse response = authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
