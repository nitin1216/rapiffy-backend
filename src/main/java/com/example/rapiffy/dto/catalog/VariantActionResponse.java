package com.example.rapiffy.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class VariantActionResponse {

    private Long parentShopProductId;
    private String message;
    private List<VariantInfo> variants;

    @Data
    @AllArgsConstructor
    public static class VariantInfo {
        private Long variantId;
        private Long shopProductId;
        private String variantName;
    }
}
