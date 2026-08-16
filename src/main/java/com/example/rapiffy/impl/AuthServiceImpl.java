package com.example.rapiffy.impl;

import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.dto.LoginRequest;
import com.example.rapiffy.dto.LoginResponse;
import com.example.rapiffy.dto.RefreshTokenRequest;
import com.example.rapiffy.dto.SignUpRequest;
import com.example.rapiffy.dto.SignUpResponse;
import com.example.rapiffy.enums.AuthProvider;
import com.example.rapiffy.model.Profile;
import com.example.rapiffy.model.RefreshToken;
import com.example.rapiffy.model.User;
import com.example.rapiffy.repos.ProfileRepository;
import com.example.rapiffy.repos.UserRepository;
import com.example.rapiffy.security.JwtUtil;
import com.example.rapiffy.services.AuthService;
import com.example.rapiffy.services.RefreshTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserRepository userRepository,
                           ProfileRepository profileRepository,
                           JwtUtil jwtUtil,
                           RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
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
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setAuthProvider(AuthProvider.NORMAL);

        User savedUser = userRepository.save(user);

        // 3. Create empty Profile linked to user
        Profile profile = new Profile();
        profile.setUser(savedUser);
        profileRepository.save(profile);

        // 4. Generate tokens and return
        String accessToken = jwtUtil.generateToken(savedUser.getPhoneNumber(), savedUser.getRole().name());
        RefreshToken refreshToken = refreshTokenService.create(savedUser);
        return new SignUpResponse(accessToken, refreshToken.getToken(), "Signup successful");
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

        // 3. Generate tokens and return
        String accessToken = jwtUtil.generateToken(user.getPhoneNumber(), user.getRole().name());
        RefreshToken refreshToken = refreshTokenService.create(user);
        return new LoginResponse(accessToken, refreshToken.getToken(), "Login successful");
    }

    @Override
    public LoginResponse refresh(RefreshTokenRequest request) {
        RefreshToken existing = refreshTokenService.validate(request.getRefreshToken());
        User user = existing.getUser();

        // Rotate — delete old, issue new refresh token
        RefreshToken newRefreshToken = refreshTokenService.create(user);
        String identifier = user.getPhoneNumber() != null ? user.getPhoneNumber() : user.getEmail();
        String accessToken = jwtUtil.generateToken(identifier, user.getRole().name());
        return new LoginResponse(accessToken, newRefreshToken.getToken(), "Token refreshed");
    }

    @Override
    public void logout(RefreshTokenRequest request) {
        RefreshToken token = refreshTokenService.validate(request.getRefreshToken());
        refreshTokenService.deleteByUser(token.getUser());
    }
}
