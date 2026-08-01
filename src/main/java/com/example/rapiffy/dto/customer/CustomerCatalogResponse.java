package com.example.rapiffy.dto.customer;

import lombok.Data;

import java.util.List;

@Data
public class CustomerCatalogResponse {

    private Long categoryId;
    private String categoryName;
    private List<SubCategoryResponse> subCategories;

    @Data
    public static class SubCategoryResponse {
        private Long subCategoryId;
        private String subCategoryName;
        private String imageUrl;
        private List<CustomerProductResponse> products;
    }
}
