package com.example.rapiffy.controller;

import com.example.rapiffy.dto.catalog.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin Catalog Controller — APIs for shopkeeper to manage their product catalog.
 *
 * Flow:
 * 1. GET  /catalog         → Browse all products from master catalog (with active/inactive status)
 * 2. POST /catalog/activate    → Turn ON a product (start selling) with price/stock
 * 3. PUT  /catalog/update/{id} → Update price, stock, name, etc. of an active product
 * 4. PUT  /catalog/deactivate/{id} → Turn OFF a product (stop selling)
 * 5. POST /catalog/add-unlisted    → Add a custom product not in master catalog
 */
@Tag(name = "Admin Catalog", description = "Shopkeeper product catalog management APIs")
@RequestMapping("v1/admin/catalog")
public interface AdminCatalogController {

    // ── BROWSE CATALOG ───────────────────────────────────────────────────────

    @Operation(
        summary = "Get catalog for Admin's shop",
        description = "Returns all MasterProducts from Admin's selected categories. "
            + "Each product shows whether it's activated in this shop or not."
    )
    @GetMapping
    ResponseEntity<List<CatalogProductResponse>> getCatalog();

    // ── ACTIVATE PRODUCT (true — start selling) ──────────────────────────────

    @Operation(
        summary = "Activate a product from catalog",
        description = "Admin turns ON a MasterProduct in their shop. "
            + "Sets selling price, stock, and optional overrides (name, description, etc.)"
    )
    @PostMapping("/activate")
    ResponseEntity<CatalogActionResponse> activateProduct(@RequestBody ActivateProductRequest request);

    // ── UPDATE PRODUCT ───────────────────────────────────────────────────────

    @Operation(
        summary = "Update an active shop product",
        description = "Admin updates price, stock, name, description, etc. of a product they're selling."
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
        description = "Admin adds a product that doesn't exist in the master catalog. "
            + "All details must be provided manually."
    )
    @PostMapping("/add-unlisted")
    ResponseEntity<CatalogActionResponse> addUnlistedProduct(@RequestBody AddUnlistedProductRequest request);

    // ── MY PRODUCTS ──────────────────────────────────────────────────────────

    @Operation(
        summary = "Get all shop products grouped by category",
        description = "Returns Admin's products (activated + unlisted) grouped by category. "
            + "e.g. Grocery: [product1, product2], Medical: [product3, product4]"
    )
    @GetMapping("/my-products")
    ResponseEntity<List<CategoryProductsResponse>> getMyProducts();

    @Operation(summary = "Get shop products filtered by category")
    @GetMapping("/my-products/{categoryId}")
    ResponseEntity<CategoryProductsResponse> getMyProductsByCategory(@PathVariable Long categoryId);
}
