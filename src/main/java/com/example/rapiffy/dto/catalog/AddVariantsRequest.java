package com.example.rapiffy.dto.catalog;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AddVariantsRequest {

    @NotNull(message = "parentShopProductId is required")
    private Long parentShopProductId;

    @NotEmpty(message = "variants list cannot be empty")
    private List<VariantRequest> variants;
}
