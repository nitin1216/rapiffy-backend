package com.example.rapiffy.dto.catalog;

import lombok.Data;

import java.util.List;

@Data
public class CategoryProductsResponse {

    private Long categoryId;
    private String categoryName;
    private List<SubCategoryProductsResponse> subCategories;

    @Data
    public static class SubCategoryProductsResponse {
        private Long subCategoryId;
        private String subCategoryName;
        private List<ShopProductResponse> products;
    }
}
