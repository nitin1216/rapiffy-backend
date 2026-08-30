package com.example.rapiffy.controller;

import com.example.rapiffy.dto.admin.AdminProfileResponse;
import com.example.rapiffy.dto.superadmin.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

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

    // ── SUBCATEGORY MANAGEMENT ────────────────────────────────────────────────

    @Operation(summary = "Create a new subCategory under a category")
    @PostMapping("/sub-category")
    ResponseEntity<SuperAdminActionResponse> createSubCategory(@RequestBody CreateSubCategoryRequest request);

    @Operation(summary = "Get all subCategories, optionally filter by categoryId")
    @GetMapping("/sub-categories")
    ResponseEntity<List<SubCategoryResponse>> getSubCategories(@RequestParam(required = false) Long categoryId);

    @Operation(summary = "Deactivate a subCategory")
    @PutMapping("/sub-category/deactivate/{subCategoryId}")
    ResponseEntity<SuperAdminActionResponse> deactivateSubCategory(@PathVariable Long subCategoryId);

    // ── MASTER PRODUCT CATALOG ───────────────────────────────────────────────

    @Operation(summary = "Add a single master product")
    @PostMapping("/catalog/product")
    ResponseEntity<SuperAdminActionResponse> addMasterProduct(@RequestBody AddMasterProductRequest request);

    @Operation(summary = "Get all master products")
    @GetMapping("/catalog/products")
    ResponseEntity<List<MasterProductResponse>> getAllMasterProducts();

    @Operation(
        summary = "Import products via CSV into a subCategory",
        description = "CSV columns: productCode, productName, brand, unit, unitValue, mrp, "
            + "shortDescription, longDescription. "
            + "Duplicate productCodes are skipped."
    )
    @PostMapping(value = "/catalog/import/{categoryId}/{subCategoryId}", consumes = "multipart/form-data")
    ResponseEntity<CsvImportResponse> importCatalog(
        @PathVariable Long categoryId,
        @PathVariable Long subCategoryId,
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

    @Operation(summary = "Remove an Admin (shopkeeper)",
        description = "Deactivates Admin account and their shop. Products will no longer be visible to customers."
    )
    @DeleteMapping("/remove-admin/{adminUserId}")
    ResponseEntity<SuperAdminActionResponse> removeAdmin(@PathVariable Long adminUserId);

    // ── RAZORPAY LINKING ─────────────────────────────────────────────────────

    @Operation(summary = "Link a shop (Admin) with Razorpay",
        description = "Creates a Razorpay linked account for the shop and saves the account ID. Call after onboarding or as a retry for existing shops."
    )
    @PostMapping("/razorpay/link-shop")
    ResponseEntity<SuperAdminActionResponse> linkShopRazorpayAccount(@RequestBody LinkShopRazorpayRequest request);

    // ── MASTER VARIANTS ───────────────────────────────────────────────────────

    @Operation(
        summary = "Add variants to a master product",
        description = "Pass parentMasterProductId and a list of variants. Each variant is treated as a first-class product."
    )
    @PostMapping("/catalog/variants")
    ResponseEntity<MasterVariantActionResponse> addMasterVariants(@RequestBody AddMasterVariantsRequest request);

    @Operation(
        summary = "Update a master variant",
        description = "Update any field of a specific master variant. Only send fields you want to change."
    )
    @PutMapping("/catalog/variants/{variantId}")
    ResponseEntity<SuperAdminActionResponse> updateMasterVariant(
        @PathVariable Long variantId,
        @RequestBody MasterVariantRequest request
    );

    @Operation(summary = "Get all variants for a master product")
    @GetMapping("/catalog/{masterProductId}/variants")
    ResponseEntity<MasterProductResponse> getMasterProductWithVariants(@PathVariable Long masterProductId);

    @Operation(summary = "Delete a master variant by variantId")
    @DeleteMapping("/catalog/variants/{variantId}")
    ResponseEntity<SuperAdminActionResponse> deleteMasterVariant(@PathVariable Long variantId);

    // ── IMAGE UPLOAD ──────────────────────────────────────────────────────────

    @Operation(summary = "Upload image for a category (replaces existing)")
    @PostMapping(value = "/category/{categoryId}/image", consumes = "multipart/form-data")
    ResponseEntity<Map<String, String>> uploadCategoryImage(
        @PathVariable Long categoryId,
        @RequestParam("file") MultipartFile file
    );

    @Operation(summary = "Upload images for a master product (appends to existing)")
    @PostMapping(value = "/catalog/product/{productId}/images", consumes = "multipart/form-data")
    ResponseEntity<Map<String, Object>> uploadProductImages(
        @PathVariable Long productId,
        @RequestParam("files") List<MultipartFile> files
    );

    @Operation(summary = "Upload images for a master variant (appends to existing)")
    @PostMapping(value = "/catalog/variant/{variantId}/images", consumes = "multipart/form-data")
    ResponseEntity<Map<String, Object>> uploadVariantImages(
        @PathVariable Long variantId,
        @RequestParam("files") List<MultipartFile> files
    );
}
