package com.example.rapiffy.controller;

import com.example.rapiffy.dto.admin.AdminProfileResponse;
import com.example.rapiffy.dto.admin.UpdateAdminProfileRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin Profile Controller — APIs for shopkeeper to view/update their profile.
 *
 * Rules:
 * - Phone number: NOT editable (used for login)
 * - Bank details: NOT editable by Admin (only SuperAdmin can update, shown masked)
 * - Email update syncs to User table
 */
@Tag(name = "Admin Profile", description = "Shopkeeper profile management APIs")
@RequestMapping("v1/admin/profile")
public interface AdminProfileController {

    @Operation(
        summary = "Get Admin's profile",
        description = "Returns full profile with masked bank details (last 4 digits only)."
    )
    @GetMapping
    ResponseEntity<AdminProfileResponse> getProfile();

    @Operation(
        summary = "Update Admin's profile",
        description = "Admin updates personal details, address, shop info. "
            + "Phone and bank details cannot be changed by Admin."
    )
    @PutMapping
    ResponseEntity<AdminProfileResponse> updateProfile(@RequestBody UpdateAdminProfileRequest request);
}
