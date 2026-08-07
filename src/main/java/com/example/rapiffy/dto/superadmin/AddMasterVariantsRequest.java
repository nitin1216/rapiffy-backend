package com.example.rapiffy.dto.superadmin;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AddMasterVariantsRequest {

    @NotNull(message = "parentMasterProductId is required")
    private Long parentMasterProductId;

    @NotEmpty(message = "variants list cannot be empty")
    private List<MasterVariantRequest> variants;
}
