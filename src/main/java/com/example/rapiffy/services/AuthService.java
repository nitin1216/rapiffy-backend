package com.example.rapiffy.services;

import com.example.rapiffy.dto.LoginRequest;
import com.example.rapiffy.dto.LoginResponse;
import com.example.rapiffy.dto.SignUpRequest;
import com.example.rapiffy.dto.SignUpResponse;
public interface AuthService {

    SignUpResponse signUp(SignUpRequest request);
    LoginResponse login(LoginRequest request);
}
