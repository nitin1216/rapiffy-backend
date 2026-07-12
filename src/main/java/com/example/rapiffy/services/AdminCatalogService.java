package com.example.rapiffy.services;

import com.example.rapiffy.dto.catalog.*;

import java.util.List;

/**
 * Service interface for Admin catalog operations.
 * All methods require the authenticated Admin's userId to identify their shop.
 */
public interface AdminCatalogService {

    // Get all catalog products for this Admin's shop categories (with active/inactive status)
    List<CatalogProductResponse> getCatalog(Long userId);

    // Activate a MasterProduct in Admin's shop
    CatalogActionResponse activateProduct(Long userId, ActivateProductRequest request);

    // Update an existing ShopProduct
    CatalogActionResponse updateProduct(Long userId, Long shopProductId, UpdateProductRequest request);

    // Deactivate a ShopProduct (stop selling)
    CatalogActionResponse deactivateProduct(Long userId, Long shopProductId);

    // Add a custom/unlisted product
    CatalogActionResponse addUnlistedProduct(Long userId, AddUnlistedProductRequest request);

    // Get all shop products (activated + unlisted)
    List<ShopProductResponse> getMyProducts(Long userId);
}
