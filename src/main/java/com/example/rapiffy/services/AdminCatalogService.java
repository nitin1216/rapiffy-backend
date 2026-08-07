package com.example.rapiffy.services;

import com.example.rapiffy.dto.catalog.*;

import java.util.List;

public interface AdminCatalogService {

    List<CategoryProductsResponse> getMyProducts(Long userId);

    CategoryProductsResponse getMyProductsBySubCategory(Long userId, Long subCategoryId);

    CatalogActionResponse updateProduct(Long userId, Long shopProductId, UpdateProductRequest request);

    CatalogActionResponse setProductVisibility(Long userId, Long shopProductId, boolean active);

    CatalogActionResponse addUnlistedProduct(Long userId, AddUnlistedProductRequest request);

    VariantActionResponse addVariants(Long userId, AddVariantsRequest request);

    CatalogActionResponse updateVariant(Long userId, Long variantId, VariantRequest request);

    CatalogActionResponse deleteVariant(Long userId, Long variantId);
}
