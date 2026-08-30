package com.example.rapiffy.impl;

import com.example.rapiffy.controller.AdminCatalogController;
import com.example.rapiffy.dto.catalog.*;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.User;
import com.example.rapiffy.model.Profile;
import com.example.rapiffy.repos.ProfileRepository;
import com.example.rapiffy.repos.UserRepository;
import com.example.rapiffy.services.AdminCatalogService;
import com.example.rapiffy.sftp.ImageUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
public class AdminCatalogControllerImpl implements AdminCatalogController {

    private final AdminCatalogService catalogService;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ImageUploadService imageUploadService;

    public AdminCatalogControllerImpl(AdminCatalogService catalogService,
                                      UserRepository userRepository,
                                      ProfileRepository profileRepository,
                                      ImageUploadService imageUploadService) {
        this.catalogService = catalogService;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.imageUploadService = imageUploadService;
    }

    @Override
    public ResponseEntity<List<CategoryProductsResponse>> getMyProducts() {
        return ResponseEntity.ok(catalogService.getMyProducts(getCurrentUserId()));
    }

    @Override
    public ResponseEntity<CategoryProductsResponse> getMyProductsBySubCategory(Long subCategoryId) {
        return ResponseEntity.ok(catalogService.getMyProductsBySubCategory(getCurrentUserId(), subCategoryId));
    }

    @Override
    public ResponseEntity<CatalogActionResponse> updateProduct(Long shopProductId, UpdateProductRequest request) {
        return ResponseEntity.ok(catalogService.updateProduct(getCurrentUserId(), shopProductId, request));
    }

    @Override
    public ResponseEntity<CatalogActionResponse> setProductVisibility(Long shopProductId, boolean active) {
        return ResponseEntity.ok(catalogService.setProductVisibility(getCurrentUserId(), shopProductId, active));
    }

    @Override
    public ResponseEntity<CatalogActionResponse> addUnlistedProduct(AddUnlistedProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(catalogService.addUnlistedProduct(getCurrentUserId(), request));
    }

    @Override
    public ResponseEntity<VariantActionResponse> addVariants(AddVariantsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(catalogService.addVariants(getCurrentUserId(), request));
    }

    @Override
    public ResponseEntity<CatalogActionResponse> updateVariant(Long variantId, VariantRequest request) {
        return ResponseEntity.ok(catalogService.updateVariant(getCurrentUserId(), variantId, request));
    }

    @Override
    public ResponseEntity<CatalogActionResponse> deleteVariant(Long variantId) {
        return ResponseEntity.ok(catalogService.deleteVariant(getCurrentUserId(), variantId));
    }

    @Override
    public ResponseEntity<Map<String, Object>> uploadShopProductImages(Long shopProductId, List<MultipartFile> files) {
        Long shopId = getCurrentShopId();
        List<String> imageUrls = imageUploadService.uploadShopProductImages(shopProductId, shopId, files);
        return ResponseEntity.ok(Map.of("shopProductId", shopProductId, "images", imageUrls));
    }

    @Override
    public ResponseEntity<Map<String, Object>> uploadShopVariantImages(Long variantId, List<MultipartFile> files) {
        Long shopId = getCurrentShopId();
        List<String> imageUrls = imageUploadService.uploadShopVariantImages(variantId, shopId, files);
        return ResponseEntity.ok(Map.of("variantId", variantId, "images", imageUrls));
    }

    private Long getCurrentUserId() {
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByPhoneNumber(identifier)
            .or(() -> userRepository.findByEmail(identifier))
            .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
        return user.getId();
    }

    private Long getCurrentShopId() {
        Long userId = getCurrentUserId();
        return profileRepository.findByUserId(userId)
            .orElseThrow(() -> new ApiException("Shop profile not found", HttpStatus.NOT_FOUND))
            .getId();
    }
}
