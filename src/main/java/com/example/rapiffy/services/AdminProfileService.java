package com.example.rapiffy.services;

import com.example.rapiffy.dto.admin.AdminProfileResponse;
import com.example.rapiffy.dto.admin.UpdateAdminProfileRequest;

public interface AdminProfileService {

    AdminProfileResponse getProfile(Long userId);

    AdminProfileResponse updateProfile(Long userId, UpdateAdminProfileRequest request);
}
