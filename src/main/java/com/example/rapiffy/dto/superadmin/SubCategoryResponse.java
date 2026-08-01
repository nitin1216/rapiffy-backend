package com.example.rapiffy.dto.superadmin;

import com.example.rapiffy.model.SubCategory;
import lombok.Data;

@Data
public class SubCategoryResponse {

    private Long id;
    private String name;
    private String imageUrl;
    private String description;
    private Long categoryId;
    private String categoryName;
    private boolean active;

    public static SubCategoryResponse from(SubCategory sc) {
        SubCategoryResponse r = new SubCategoryResponse();
        r.setId(sc.getId());
        r.setName(sc.getName());
        r.setImageUrl(sc.getImageUrl());
        r.setDescription(sc.getDescription());
        r.setCategoryId(sc.getCategory().getId());
        r.setCategoryName(sc.getCategory().getCategoryName());
        r.setActive(sc.isActive());
        return r;
    }
}
