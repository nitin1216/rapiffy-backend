package com.example.rapiffy.services;

import com.example.rapiffy.dto.catalog.*;

import java.util.List;

public interface AdminCatalogService {

    List<CategoryProductsResponse> getMyProducts(Long userId);

    CategoryProductsResponse getMyProductsBySubCategory(Long userId, Long subCategoryId);

    CatalogActionResponse updateProduct(Long userId, Long shopProductId, UpdateProductRequest request);

    CatalogActionResponse setProductVisibility(Long userId, Long shopProductId, boolean active);

    CatalogActionResponse addUnlistedProduct(Long userId, AddUnlistedProductRequest request);
}
