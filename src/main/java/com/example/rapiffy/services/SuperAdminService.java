package com.example.rapiffy.services;

import com.example.rapiffy.dto.superadmin.*;
import com.example.rapiffy.model.Category;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service interface for SuperAdmin operations.
 */
public interface SuperAdminService {

    // Category management
    SuperAdminActionResponse createCategory(CreateCategoryRequest request);
    List<Category> getAllCategories();
    SuperAdminActionResponse deactivateCategory(Long categoryId);

    // Master product catalog
    CsvImportResponse importCatalog(Long categoryId, MultipartFile file);
    SuperAdminActionResponse updateMasterProduct(Long masterProductId, UpdateMasterProductRequest request);

    // Admin onboarding & removal
    SuperAdminActionResponse onboardAdmin(OnboardAdminRequest request);
    SuperAdminActionResponse removeAdmin(Long adminUserId);
}
