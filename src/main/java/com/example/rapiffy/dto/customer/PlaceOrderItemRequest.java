package com.example.rapiffy.dto.customer;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlaceOrderItemRequest {

    @NotNull(message = "shopProductId is required")
    private Long shopProductId;

    @NotNull
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    // Which shop this product belongs to — used to group into sub-orders
    @NotNull(message = "shopId is required")
    private Long shopId;
}
