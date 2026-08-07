package com.example.rapiffy.controller;

import com.example.rapiffy.dto.platform.*;
import com.example.rapiffy.model.PlatformCommission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Platform", description = "Platform commission and Razorpay account management")
@RequestMapping("v1/platform")
public interface PlatformController {

    @Operation(summary = "Save platform commission Razorpay account",
        description = "Creates a Razorpay linked account for the platform. All customerCommission + shopCommission from every order will be routed here. Call ONCE during platform setup.")
    @PostMapping("/commission-account")
    ResponseEntity<PlatformActionResponse> saveCommissionAccount(@RequestBody SaveCommissionAccountRequest request);

    @Operation(summary = "Set commission rates for a category",
        description = "Set both customer-side and shop-side commission % for a specific category. Creates new or updates existing.")
    @PostMapping("/commission-rate")
    ResponseEntity<PlatformActionResponse> setCommissionRate(@RequestBody SetCommissionRateRequest request);

    @Operation(summary = "Deactivate commission rate for a category",
        description = "Falls back to default commission rate after deactivation.")
    @PutMapping("/commission-rate/deactivate/{categoryId}")
    ResponseEntity<PlatformActionResponse> deactivateCommissionRate(@PathVariable Long categoryId);

    @Operation(summary = "Get all commission rates")
    @GetMapping("/commission-rates")
    ResponseEntity<List<PlatformCommission>> getAllCommissionRates();

    @Operation(summary = "Update global default commission rate",
        description = "Used as fallback when no category-specific rate is set.")
    @PutMapping("/default-commission-rate")
    ResponseEntity<PlatformActionResponse> updateDefaultCommissionRate(@RequestBody UpdateDefaultCommissionRequest request);
}
