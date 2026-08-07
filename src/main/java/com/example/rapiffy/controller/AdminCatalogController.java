package com.example.rapiffy.controller;

import com.example.rapiffy.dto.catalog.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin Catalog Controller — APIs for shopkeeper to manage their shop products.
 *
 * Flow:
 * 1. GET  /my-products              → View all shop products as Category → SubCategory → Products tree
 * 2. GET  /my-products/sub-category/{subCategoryId} → Filter by subCategory
 * 3. PUT  /update/{shopProductId}   → Update price, stock, name, etc.
 * 4. PATCH /visibility/{shopProductId} → Show/hide product from customers
 * 5. POST /add-unlisted             → Add a custom product not in master catalog
 */
@Tag(name = "Admin Catalog", description = "Shopkeeper product catalog management APIs")
@RequestMapping("v1/admin/catalog")
public interface AdminCatalogController {

    // ── MY PRODUCTS ──────────────────────────────────────────────────────────

    @Operation(
        summary = "Get all shop products as tree",
        description = "Returns Admin's active products grouped as Category → SubCategory → Products."
    )
    @GetMapping("/my-products")
    ResponseEntity<List<CategoryProductsResponse>> getMyProducts();

    @Operation(summary = "Get shop products filtered by subCategory")
    @GetMapping("/my-products/sub-category/{subCategoryId}")
    ResponseEntity<CategoryProductsResponse> getMyProductsBySubCategory(@PathVariable Long subCategoryId);

    // ── UPDATE PRODUCT ───────────────────────────────────────────────────────

    @Operation(
        summary = "Update a shop product",
        description = "Admin updates price, stock, name, description, etc."
    )
    @PutMapping("/update/{shopProductId}")
    ResponseEntity<CatalogActionResponse> updateProduct(
        @PathVariable Long shopProductId,
        @RequestBody UpdateProductRequest request
    );

    // ── TOGGLE VISIBILITY ────────────────────────────────────────────────────

    @Operation(
        summary = "Toggle product visibility",
        description = "active=true → product visible to customers. active=false → hidden from customers."
    )
    @PatchMapping("/visibility/{shopProductId}")
    ResponseEntity<CatalogActionResponse> setProductVisibility(
        @PathVariable Long shopProductId,
        @RequestParam boolean active
    );

    // ── ADD UNLISTED PRODUCT ─────────────────────────────────────────────────

    @Operation(
        summary = "Add an unlisted/custom product",
        description = "Admin adds a product that doesn't exist in the master catalog."
    )
    @PostMapping("/add-unlisted")
    ResponseEntity<CatalogActionResponse> addUnlistedProduct(@RequestBody AddUnlistedProductRequest request);

    // ── ADD VARIANTS ─────────────────────────────────────────────────────────

    @Operation(
        summary = "Add variants to a shop product",
        description = "Pass parentShopProductId and a list of variants. Each variant gets its own shopProductId."
    )
    @PostMapping("/variants")
    ResponseEntity<VariantActionResponse> addVariants(@RequestBody AddVariantsRequest request);

    // ── UPDATE VARIANT ───────────────────────────────────────────────────────

    @Operation(
        summary = "Update a variant",
        description = "Update any field of a specific variant by variantId. Only send fields you want to change."
    )
    @PutMapping("/variants/{variantId}")
    ResponseEntity<CatalogActionResponse> updateVariant(
        @PathVariable Long variantId,
        @RequestBody VariantRequest request
    );

    // ── DELETE VARIANT ───────────────────────────────────────────────────────

    @Operation(summary = "Delete a variant by variantId")
    @DeleteMapping("/variants/{variantId}")
    ResponseEntity<CatalogActionResponse> deleteVariant(@PathVariable Long variantId);
}
