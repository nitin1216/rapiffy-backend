package com.example.rapiffy.impl;

import com.example.rapiffy.controller.AdminCancellationController;
import com.example.rapiffy.dto.admin.cancellation.AdminReviewCancellationRequest;
import com.example.rapiffy.dto.customer.cancellation.CancellationRequestResponse;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.Profile;
import com.example.rapiffy.model.User;
import com.example.rapiffy.repos.ProfileRepository;
import com.example.rapiffy.repos.UserRepository;
import com.example.rapiffy.services.AdminCancellationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminCancellationControllerImpl implements AdminCancellationController {

    private final AdminCancellationService adminCancellationService;
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    @Override
    public ResponseEntity<List<CancellationRequestResponse>> getCancellations() {
        return ResponseEntity.ok(adminCancellationService.getCancellations(getCurrentShopId()));
    }

    @Override
    public ResponseEntity<CancellationRequestResponse> reviewCancellation(Long cancellationRequestId, AdminReviewCancellationRequest request) {
        return ResponseEntity.ok(adminCancellationService.reviewCancellation(getCurrentShopId(), cancellationRequestId, request));
    }

    private Long getCurrentShopId() {
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByPhoneNumber(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
        Profile shop = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException("Shop profile not found", HttpStatus.NOT_FOUND));
        return shop.getId();
    }
}
