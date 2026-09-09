package com.example.rapiffy.dto.admin.cancellation;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminReviewCancellationRequest {

    @NotNull(message = "Approval decision is required")
    private Boolean approved;

    // Required when rejecting, optional when approving
    private String adminNote;
}
