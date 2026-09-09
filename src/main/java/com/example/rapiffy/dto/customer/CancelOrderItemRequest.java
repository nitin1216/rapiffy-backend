package com.example.rapiffy.dto.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CancelOrderItemRequest {

    @NotBlank(message = "Reason is required")
    private String reason;

    @NotEmpty(message = "At least one item is required")
    private List<CancelItemEntry> items;

    @Data
    public static class CancelItemEntry {
        private Long orderItemId;
        private Integer quantityToCancel;
    }
}
