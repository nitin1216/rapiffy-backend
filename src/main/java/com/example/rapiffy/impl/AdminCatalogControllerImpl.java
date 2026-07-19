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
    public ResponseEntity<List<CatalogProductResponse>> getCatalog() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(catalogService.getCatalog(userId));
    }

    @Override
    public ResponseEntity<CatalogActionResponse> activateProduct(ActivateProductRequest request) {
        Long userId = getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(catalogService.activateProduct(userId, request));
    }

    @Override
    public ResponseEntity<CatalogActionResponse> updateProduct(Long shopProductId, UpdateProductRequest request) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(catalogService.updateProduct(userId, shopProductId, request));
    }

    @Override
    public ResponseEntity<CatalogActionResponse> setProductVisibility(Long shopProductId, boolean active) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(catalogService.setProductVisibility(userId, shopProductId, active));
    }

    @Override
    public ResponseEntity<CatalogActionResponse> addUnlistedProduct(AddUnlistedProductRequest request) {
        Long userId = getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(catalogService.addUnlistedProduct(userId, request));
    }

    @Override
    public ResponseEntity<List<CategoryProductsResponse>> getMyProducts() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(catalogService.getMyProducts(userId));
    }

    @Override
    public ResponseEntity<CategoryProductsResponse> getMyProductsByCategory(Long categoryId) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(catalogService.getMyProductsByCategory(userId, categoryId));
    }

    // ── HELPER: Extract current logged-in user's ID from JWT/SecurityContext ─

    private Long getCurrentUserId() {
        // SecurityContext principal = identifier (phone or email) set by JwtAuthFilter
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();

        // Find user by phone or email
        User user = userRepository.findByPhoneNumber(identifier)
            .or(() -> userRepository.findByEmail(identifier))
            .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));

        return user.getId();
    }
}
