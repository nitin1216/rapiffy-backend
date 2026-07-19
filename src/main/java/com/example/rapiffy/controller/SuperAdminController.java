package com.example.rapiffy.controller;

import com.example.rapiffy.dto.admin.AdminProfileResponse;
import com.example.rapiffy.dto.superadmin.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * SuperAdmin Controller — Platform-level management APIs.
 *
 * SuperAdmin responsibilities:
 * 1. Create & manage categories (Grocery, Medical, Fashion...)
 * 2. Import & manage master product catalog (CSV + variants)
 * 3. Onboard/remove Admin (shopkeeper)
 */
@Tag(name = "Super Admin", description = "Platform management APIs (categories, catalog, onboarding)")
@RequestMapping("v1/super-admin")
public interface SuperAdminController {

    // ── CATEGORY MANAGEMENT ──────────────────────────────────────────────────

    @Operation(summary = "Create a new category")
    @PostMapping("/category")
    ResponseEntity<SuperAdminActionResponse> createCategory(@RequestBody CreateCategoryRequest request);

    @Operation(summary = "Get all categories")
    @GetMapping("/categories")
    ResponseEntity<List<com.example.rapiffy.model.Category>> getAllCategories();

    @Operation(summary = "Deactivate a category")
    @PutMapping("/category/deactivate/{categoryId}")
    ResponseEntity<SuperAdminActionResponse> deactivateCategory(@PathVariable Long categoryId);

    // ── MASTER PRODUCT CATALOG ───────────────────────────────────────────────

    @Operation(summary = "Add a single master product")
    @PostMapping("/catalog/product")
    ResponseEntity<SuperAdminActionResponse> addMasterProduct(@RequestBody AddMasterProductRequest request);

    @Operation(summary = "Get all master products", description = "Pass categoryId to filter by category, or omit for all.")
    @GetMapping("/catalog/products")
    ResponseEntity<List<MasterProductResponse>> getAllMasterProducts(
        @RequestParam(required = false) Long categoryId
    );

    @Operation(
        summary = "Import products via CSV into a category",
        description = "CSV columns: productCode, productName, brand, unit, unitValue, mrp, "
            + "shortDescription, longDescription, attributes (JSON). "
            + "Duplicate productCodes are skipped."
    )
    @PostMapping(value = "/catalog/import/{categoryId}", consumes = "multipart/form-data")
    ResponseEntity<CsvImportResponse> importCatalog(
        @PathVariable Long categoryId,
        @RequestParam("file") MultipartFile file
    );

    @Operation(
        summary = "Update a master product (including variants)",
        description = "Update product details and manage variants. "
            + "Variants with id = update, without id = add new, missing from list = removed."
    )
    @PutMapping("/catalog/{masterProductId}")
    ResponseEntity<SuperAdminActionResponse> updateMasterProduct(
        @PathVariable Long masterProductId,
        @RequestBody UpdateMasterProductRequest request
    );

    @Operation(summary = "Add a master product to a specific Admin's shop")
    @PostMapping("/catalog/add-to-shop")
    ResponseEntity<SuperAdminActionResponse> addProductToShop(@RequestBody AddProductToShopRequest request);

    // ── ADMIN (SHOPKEEPER) MANAGEMENT ────────────────────────────────────────

    @Operation(summary = "Get all onboarded Admins")
    @GetMapping("/admins")
    ResponseEntity<List<AdminProfileResponse>> getAllAdmins();

    @Operation(summary = "Get a specific Admin's profile by phone")
    @GetMapping("/admin/{phoneNumber}")
    ResponseEntity<AdminProfileResponse> getAdminProfile(@PathVariable String phoneNumber);

    @Operation(
        summary = "Onboard a new Admin (shopkeeper)",
        description = "Creates User account with ADMIN role + Profile with shop details and assigned categories."
    )
    @PostMapping("/onboard-admin")
    ResponseEntity<SuperAdminActionResponse> onboardAdmin(@RequestBody OnboardAdminRequest request);

    @Operation(
        summary = "Update Admin (shopkeeper) profile",
        description = "All fields optional — only non-null fields are updated. Send only what you want to change."
    )
    @PutMapping("/admin/{phoneNumber}/profile")
    ResponseEntity<SuperAdminActionResponse> updateAdminProfile(
        @PathVariable String phoneNumber,
        @RequestBody UpdateAdminProfileBySuperAdminRequest request
    );

    @Operation(
        summary = "Remove an Admin (shopkeeper)",
        description = "Deactivates Admin account and their shop. Products will no longer be visible to customers."
    )
    @DeleteMapping("/remove-admin/{adminUserId}")
    ResponseEntity<SuperAdminActionResponse> removeAdmin(@PathVariable Long adminUserId);
}
