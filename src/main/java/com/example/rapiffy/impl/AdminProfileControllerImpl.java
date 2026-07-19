package com.example.rapiffy.impl;

import com.example.rapiffy.controller.AdminProfileController;
import com.example.rapiffy.dto.admin.AdminProfileResponse;
import com.example.rapiffy.dto.admin.UpdateAdminProfileRequest;
import com.example.rapiffy.dto.admin.UpdateShopLocationRequest;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.User;
import com.example.rapiffy.repos.UserRepository;
import com.example.rapiffy.services.AdminProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminProfileControllerImpl implements AdminProfileController {

    private final AdminProfileService profileService;
    private final UserRepository userRepository;

    public AdminProfileControllerImpl(AdminProfileService profileService,
                                      UserRepository userRepository) {
        this.profileService = profileService;
        this.userRepository = userRepository;
    }

    @Override
    public ResponseEntity<AdminProfileResponse> getProfile() {
        return ResponseEntity.ok(profileService.getProfile(getCurrentUserId()));
    }

    @Override
    public ResponseEntity<AdminProfileResponse> updateProfile(UpdateAdminProfileRequest request) {
        return ResponseEntity.ok(profileService.updateProfile(getCurrentUserId(), request));
    }

    @Override
    public ResponseEntity<Void> updateShopLocation(UpdateShopLocationRequest request) {
        profileService.updateShopLocation(getCurrentUserId(), request);
        return ResponseEntity.ok().build();
    }

    private Long getCurrentUserId() {
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByPhoneNumber(identifier)
            .or(() -> userRepository.findByEmail(identifier))
            .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
        return user.getId();
    }
}
