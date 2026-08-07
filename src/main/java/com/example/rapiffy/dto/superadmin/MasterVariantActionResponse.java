package com.example.rapiffy.dto.superadmin;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class MasterVariantActionResponse {

    private Long parentMasterProductId;
    private String message;
    private List<VariantInfo> variants;

    @Data
    @AllArgsConstructor
    public static class VariantInfo {
        private Long variantId;
        private String variantName;
    }
}
