package com.example.rapiffy.services;

import com.example.rapiffy.dto.admin.AdminProfileResponse;
import com.example.rapiffy.dto.admin.UpdateAdminProfileRequest;
import com.example.rapiffy.dto.admin.UpdateShopLocationRequest;

public interface AdminProfileService {

    AdminProfileResponse getProfile(Long userId);

    AdminProfileResponse updateProfile(Long userId, UpdateAdminProfileRequest request);

    void updateShopLocation(Long userId, UpdateShopLocationRequest request);
}
