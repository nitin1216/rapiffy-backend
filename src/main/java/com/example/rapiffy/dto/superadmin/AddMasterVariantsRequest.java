package com.example.rapiffy.dto.superadmin;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AddMasterVariantsRequest {

    @NotNull(message = "parentMasterProductId is required")
    private Long parentMasterProductId;

    // e.g. ["Size", "Colour"] — defines what dimensions variants vary on
    @NotEmpty(message = "attributeTypes cannot be empty")
    private List<String> attributeTypes;

    @NotEmpty(message = "variants list cannot be empty")
    private List<MasterVariantRequest> variants;
}
