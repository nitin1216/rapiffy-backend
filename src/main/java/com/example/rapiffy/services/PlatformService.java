package com.example.rapiffy.services;

import com.example.rapiffy.dto.platform.*;
import com.example.rapiffy.model.PlatformCommission;

import java.util.List;

public interface PlatformService {

    // Commission account (Razorpay linked account for platform earnings)
    PlatformActionResponse saveCommissionAccount(SaveCommissionAccountRequest request);

    // Per-category commission rates
    PlatformActionResponse setCommissionRate(SetCommissionRateRequest request);
    PlatformActionResponse deactivateCommissionRate(Long categoryId);
    List<PlatformCommission> getAllCommissionRates();

    // Global default commission rate
    PlatformActionResponse updateDefaultCommissionRate(UpdateDefaultCommissionRequest request);
}
