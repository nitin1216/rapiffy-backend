package com.example.rapiffy.dto.superadmin;

import lombok.Data;

@Data
public class CreateSubCategoryRequest {

    private Long categoryId;
    private String name;
    private String imageUrl;
    private String description;
}
