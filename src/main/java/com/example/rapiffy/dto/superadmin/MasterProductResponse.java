package com.example.rapiffy.dto.superadmin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.example.rapiffy.model.MasterProduct;
import com.example.rapiffy.model.MasterProductVariant;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class MasterProductResponse {

    private Long id;
    private String productCode;
    private String productName;
    private Long subCategoryId;
    private String subCategoryName;
    private Long categoryId;
    private String categoryName;
    private String brand;
    private String unit;
    private String unitValue;
    private Double mrp;
    private String imageUrl;
    private String shortDescription;
    private String longDescription;
    private boolean hasVariants;
    private boolean active;
    private LocalDateTime createdAt;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<MasterProductVariant> variants;

    public static MasterProductResponse from(MasterProduct mp) {
        MasterProductResponse r = new MasterProductResponse();
        r.setId(mp.getId());
        r.setProductCode(mp.getProductCode());
        r.setProductName(mp.getProductName());
        r.setSubCategoryId(mp.getSubCategory().getId());
        r.setSubCategoryName(mp.getSubCategory().getName());
        r.setCategoryId(mp.getSubCategory().getCategory().getId());
        r.setCategoryName(mp.getSubCategory().getCategory().getCategoryName());
        r.setBrand(mp.getBrand());
        r.setUnit(mp.getUnit());
        r.setUnitValue(mp.getUnitValue());
        r.setMrp(mp.getMrp());
        r.setImageUrl(mp.getImageUrl());
        r.setShortDescription(mp.getShortDescription());
        r.setLongDescription(mp.getLongDescription());
        r.setHasVariants(mp.isHasVariants());
        r.setActive(mp.isActive());
        r.setCreatedAt(mp.getCreatedAt());
        r.setVariants(mp.getVariants());
        return r;
    }
}
