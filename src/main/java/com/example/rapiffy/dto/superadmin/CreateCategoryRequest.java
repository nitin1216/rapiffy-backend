package com.example.rapiffy.dto.superadmin;

import lombok.Data;

/**
 * Request DTO for SuperAdmin to create a new category.
 * Example: Grocery, Medical, Fashion, Dairy, Electronics
 */
@Data
public class CreateCategoryRequest {

    private String categoryCode;  // e.g. "GRO", "MED", "FSH"
    private String categoryName;  // e.g. "Grocery", "Medical"
    private String imageUrl;
    private String description;
}
