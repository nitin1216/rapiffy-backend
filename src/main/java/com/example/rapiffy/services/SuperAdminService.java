package com.example.rapiffy.services;

import com.example.rapiffy.dto.admin.AdminProfileResponse;
import com.example.rapiffy.dto.superadmin.*;
import com.example.rapiffy.model.Category;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SuperAdminService {

    // Category management
    SuperAdminActionResponse createCategory(CreateCategoryRequest request);
    List<Category> getAllCategories();
    SuperAdminActionResponse deactivateCategory(Long categoryId);

    // SubCategory management
    SuperAdminActionResponse createSubCategory(CreateSubCategoryRequest request);
    List<SubCategoryResponse> getSubCategories(Long categoryId);
    SuperAdminActionResponse deactivateSubCategory(Long subCategoryId);

    // Master product catalog
    SuperAdminActionResponse addMasterProduct(AddMasterProductRequest request);
    CsvImportResponse importCatalog(Long categoryId, Long subCategoryId, MultipartFile file);
    SuperAdminActionResponse updateMasterProduct(Long masterProductId, UpdateMasterProductRequest request);
    List<MasterProductResponse> getAllMasterProducts();
    SuperAdminActionResponse addProductToShop(AddProductToShopRequest request);

    // Admin management
    AdminProfileResponse getAdminProfile(String phoneNumber);
    List<AdminProfileResponse> getAllAdmins();
    SuperAdminActionResponse onboardAdmin(OnboardAdminRequest request);
    SuperAdminActionResponse removeAdmin(Long adminUserId);
    SuperAdminActionResponse updateAdminProfile(String phoneNumber, UpdateAdminProfileBySuperAdminRequest request);
}
