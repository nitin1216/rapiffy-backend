package com.example.rapiffy.impl;

import com.example.rapiffy.controller.AdminReturnController;
import com.example.rapiffy.dto.admin.returns.AdminReviewReturnRequest;
import com.example.rapiffy.dto.customer.returns.ReturnRequestResponse;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.Profile;
import com.example.rapiffy.model.User;
import com.example.rapiffy.repos.ProfileRepository;
import com.example.rapiffy.repos.UserRepository;
import com.example.rapiffy.services.AdminReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminReturnControllerImpl implements AdminReturnController {

    private final AdminReturnService adminReturnService;
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    @Override
    public ResponseEntity<List<ReturnRequestResponse>> getReturns() {
        return ResponseEntity.ok(adminReturnService.getReturns(getCurrentShopId()));
    }

    @Override
    public ResponseEntity<ReturnRequestResponse> reviewReturn(Long returnRequestId, AdminReviewReturnRequest request) {
        return ResponseEntity.ok(adminReturnService.reviewReturn(getCurrentShopId(), returnRequestId, request));
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
