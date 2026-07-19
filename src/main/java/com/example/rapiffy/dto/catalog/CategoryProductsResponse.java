package com.example.rapiffy.dto.catalog;

import lombok.Data;

import java.util.List;

@Data
public class CategoryProductsResponse {

    private String categoryName;
    private List<ShopProductResponse> products;
}
