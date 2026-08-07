package com.example.rapiffy.impl;

import com.example.rapiffy.controller.PlatformController;
import com.example.rapiffy.dto.platform.*;
import com.example.rapiffy.model.PlatformCommission;
import com.example.rapiffy.services.PlatformService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PlatformControllerImpl implements PlatformController {

    private final PlatformService platformService;

    @Override
    public ResponseEntity<PlatformActionResponse> saveCommissionAccount(SaveCommissionAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(platformService.saveCommissionAccount(request));
    }

    @Override
    public ResponseEntity<PlatformActionResponse> setCommissionRate(SetCommissionRateRequest request) {
        return ResponseEntity.ok(platformService.setCommissionRate(request));
    }

    @Override
    public ResponseEntity<PlatformActionResponse> deactivateCommissionRate(Long categoryId) {
        return ResponseEntity.ok(platformService.deactivateCommissionRate(categoryId));
    }

    @Override
    public ResponseEntity<List<PlatformCommission>> getAllCommissionRates() {
        return ResponseEntity.ok(platformService.getAllCommissionRates());
    }

    @Override
    public ResponseEntity<PlatformActionResponse> updateDefaultCommissionRate(UpdateDefaultCommissionRequest request) {
        return ResponseEntity.ok(platformService.updateDefaultCommissionRate(request));
    }
}
