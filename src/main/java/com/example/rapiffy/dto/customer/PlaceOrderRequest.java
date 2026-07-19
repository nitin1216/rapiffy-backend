package com.example.rapiffy.dto.customer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PlaceOrderRequest {

    // DELIVERY or SELF
    @NotNull(message = "deliveryType is required")
    private String deliveryType;

    // Optional — if not provided, backend uses customer's default saved address
    private String deliveryAddress;

    // Items from one or multiple shops — backend groups by shopId into sub-orders
    @NotEmpty(message = "Order must have at least one item")
    @Valid
    private List<PlaceOrderItemRequest> items;
}
