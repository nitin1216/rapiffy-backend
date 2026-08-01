package com.example.rapiffy.impl;

import com.example.rapiffy.dto.admin.AdminProfileResponse;
import com.example.rapiffy.controller.SuperAdminController;
import com.example.rapiffy.dto.superadmin.*;
import com.example.rapiffy.model.Category;
import com.example.rapiffy.services.SuperAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class SuperAdminControllerImpl implements SuperAdminController {

    private final SuperAdminService superAdminService;

    public SuperAdminControllerImpl(SuperAdminService superAdminService) {
        this.superAdminService = superAdminService;
    }

    // ── CATEGORY ─────────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<SuperAdminActionResponse> createCategory(CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(superAdminService.createCategory(request));
    }

    @Override
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(superAdminService.getAllCategories());
    }

    @Override
    public ResponseEntity<SuperAdminActionResponse> deactivateCategory(Long categoryId) {
        return ResponseEntity.ok(superAdminService.deactivateCategory(categoryId));
    }

    // ── SUBCATEGORY ───────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<SuperAdminActionResponse> createSubCategory(CreateSubCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(superAdminService.createSubCategory(request));
    }

    @Override
    public ResponseEntity<List<SubCategoryResponse>> getSubCategories(Long categoryId) {
        return ResponseEntity.ok(superAdminService.getSubCategories(categoryId));
    }

    @Override
    public ResponseEntity<SuperAdminActionResponse> deactivateSubCategory(Long subCategoryId) {
        return ResponseEntity.ok(superAdminService.deactivateSubCategory(subCategoryId));
    }

    // ── CATALOG ───────────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<SuperAdminActionResponse> addMasterProduct(AddMasterProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(superAdminService.addMasterProduct(request));
    }

    @Override
    public ResponseEntity<List<MasterProductResponse>> getAllMasterProducts() {
        return ResponseEntity.ok(superAdminService.getAllMasterProducts());
    }

    @Override
    public ResponseEntity<CsvImportResponse> importCatalog(Long categoryId, Long subCategoryId, MultipartFile file) {
        return ResponseEntity.ok(superAdminService.importCatalog(categoryId, subCategoryId, file));
    }

    @Override
    public ResponseEntity<SuperAdminActionResponse> updateMasterProduct(Long masterProductId, UpdateMasterProductRequest request) {
        return ResponseEntity.ok(superAdminService.updateMasterProduct(masterProductId, request));
    }

    @Override
    public ResponseEntity<SuperAdminActionResponse> addProductToShop(AddProductToShopRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(superAdminService.addProductToShop(request));
    }

    // ── ADMIN ONBOARDING ──────────────────────────────────────────────────────

    @Override
    public ResponseEntity<List<AdminProfileResponse>> getAllAdmins() {
        return ResponseEntity.ok(superAdminService.getAllAdmins());
    }

    @Override
    public ResponseEntity<AdminProfileResponse> getAdminProfile(String phoneNumber) {
        return ResponseEntity.ok(superAdminService.getAdminProfile(phoneNumber));
    }

    @Override
    public ResponseEntity<SuperAdminActionResponse> onboardAdmin(OnboardAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(superAdminService.onboardAdmin(request));
    }

    @Override
    public ResponseEntity<SuperAdminActionResponse> updateAdminProfile(String phoneNumber, UpdateAdminProfileBySuperAdminRequest request) {
        return ResponseEntity.ok(superAdminService.updateAdminProfile(phoneNumber, request));
    }

    @Override
    public ResponseEntity<SuperAdminActionResponse> removeAdmin(Long adminUserId) {
        return ResponseEntity.ok(superAdminService.removeAdmin(adminUserId));
    }
}
