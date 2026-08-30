package com.example.rapiffy.dto.superadmin;

import com.example.rapiffy.dto.catalog.VariantResponse;
import com.example.rapiffy.model.MasterProduct;
import com.example.rapiffy.model.MasterProductVariant;
import com.example.rapiffy.model.VariantAttributeType;
import com.example.rapiffy.model.VariantAttributeValue;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> imageUrls;
    private String shortDescription;
    private String longDescription;
    private boolean hasVariants;
    private boolean active;
    private LocalDateTime createdAt;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> attributeTypes;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<VariantResponse> variants;

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
        r.setImageUrls(mp.getImages().stream()
            .map(com.example.rapiffy.model.MasterProductImage::getImageUrl)
            .collect(Collectors.toList()));
        r.setShortDescription(mp.getShortDescription());
        r.setLongDescription(mp.getLongDescription());
        r.setHasVariants(mp.isHasVariants());
        r.setActive(mp.isActive());
        r.setCreatedAt(mp.getCreatedAt());
        r.setAttributeTypes(mp.getAttributeTypes().stream()
            .map(VariantAttributeType::getAttributeName)
            .collect(Collectors.toList()));
        r.setVariants(mp.getVariants().stream()
            .map(MasterProductResponse::toVariantResponse)
            .collect(Collectors.toList()));
        return r;
    }

    private static VariantResponse toVariantResponse(MasterProductVariant v) {
        Map<String, String> attributes = v.getAttributeValues().stream()
            .collect(Collectors.toMap(
                av -> av.getAttributeType().getAttributeName(),
                VariantAttributeValue::getAttributeValue
            ));

        VariantResponse vr = new VariantResponse();
        vr.setId(v.getId());
        vr.setVariantName(v.getVariantName());
        vr.setBrand(v.getBrand());
        vr.setMrp(v.getMrp());
        vr.setSellingPrice(v.getSellingPrice());
        vr.setStockQuantity(v.getStockQuantity());
        vr.setThresholdQuantity(v.getThresholdQuantity());
        vr.setImageUrl(v.getImageUrl());
        vr.setImageUrls(v.getImages().stream()
            .map(com.example.rapiffy.model.MasterProductVariantImage::getImageUrl)
            .collect(Collectors.toList()));
        vr.setExpiryDate(v.getExpiryDate());
        vr.setActive(v.isActive());
        vr.setAttributes(attributes);
        return vr;
    }
}
