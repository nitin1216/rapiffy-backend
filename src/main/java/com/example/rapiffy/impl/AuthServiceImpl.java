package com.example.rapiffy.impl;

import com.example.rapiffy.common.ApiException;
import com.example.rapiffy.dto.LoginRequest;
import com.example.rapiffy.dto.LoginResponse;
import com.example.rapiffy.dto.SignUpRequest;
import com.example.rapiffy.dto.SignUpResponse;
import com.example.rapiffy.enums.AuthProvider;
import com.example.rapiffy.model.User;
import com.example.rapiffy.model.profiles;
import com.example.rapiffy.repos.ProfileRepository;
import com.example.rapiffy.repos.UserRepository;
import com.example.rapiffy.security.JwtUtil;
import com.example.rapiffy.services.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserRepository userRepository,
                           ProfileRepository profileRepository,
                           JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public SignUpResponse signUp(SignUpRequest request) {
        // 1. Check if phone already registered
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new ApiException("Phone number already registered", HttpStatus.CONFLICT);
        }

        // 2. Build and save User
        User user = new User();
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // hash password
        user.setRole(request.getRole());
        user.setAuthProvider(AuthProvider.NORMAL);

        User savedUser = userRepository.save(user);

        // 3. Create empty Profile linked to user
        profiles profile = new profiles();
        profile.setUser(savedUser);
        profileRepository.save(profile);

        // 4. Generate JWT and return
        String token = jwtUtil.generateToken(savedUser.getPhoneNumber(), savedUser.getRole().name());
        return new SignUpResponse(token, "Signup successful");
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // 1. Find user by phone number
        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new ApiException("Phone number not registered", HttpStatus.NOT_FOUND));

        // 2. Check password matches
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ApiException("Invalid password", HttpStatus.UNAUTHORIZED);
        }

        // 3. Generate JWT and return
        String token = jwtUtil.generateToken(user.getPhoneNumber(), user.getRole().name());
        return new LoginResponse(token, "Login successful");
    }
}
