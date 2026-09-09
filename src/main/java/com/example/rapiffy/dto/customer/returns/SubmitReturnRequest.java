package com.example.rapiffy.dto.customer.returns;

import com.example.rapiffy.enums.ReturnReason;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SubmitReturnRequest {

    @NotNull(message = "Reason is required")
    private ReturnReason reason;

    // Optional note from customer describing the issue
    private String customerNote;

    @NotEmpty(message = "At least one item must be selected for return")
    @Valid
    private List<ReturnItemRequest> items;
}
