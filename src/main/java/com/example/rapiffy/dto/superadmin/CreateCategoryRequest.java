package com.example.rapiffy.dto.superadmin;

import com.example.rapiffy.enums.CategoryType;
import lombok.Data;

/**
 * Request DTO for SuperAdmin to create a new category.
 * categoryType links this DB row to the platform enum.
 */
@Data
public class CreateCategoryRequest {

    private CategoryType categoryType;
    private String imageUrl;
    private String description;
}
