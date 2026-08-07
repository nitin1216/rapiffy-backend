package com.example.rapiffy.impl;

import com.example.rapiffy.controller.AdminCatalogController;
import com.example.rapiffy.dto.catalog.*;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.User;
import com.example.rapiffy.repos.UserRepository;
import com.example.rapiffy.services.AdminCatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AdminCatalogControllerImpl implements AdminCatalogController {

    private final AdminCatalogService catalogService;
    private final UserRepository userRepository;

    public AdminCatalogControllerImpl(AdminCatalogService catalogService,
                                      UserRepository userRepository) {
        this.catalogService = catalogService;
        this.userRepository = userRepository;
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

    private Long getCurrentUserId() {
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByPhoneNumber(identifier)
            .or(() -> userRepository.findByEmail(identifier))
            .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
        return user.getId();
    }
}
