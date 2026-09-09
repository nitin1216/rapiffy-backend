package com.example.rapiffy.dto.customer;

import com.example.rapiffy.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PlaceOrderRequest {

    // Must be a saved address — ensures lat/lng is always available for delivery tracking
    @NotNull(message = "deliveryAddressId is required")
    private Long deliveryAddressId;

    // Optional instruction for the delivery person
    private String deliveryInstruction;

    @NotEmpty(message = "Order must have at least one item")
    @Valid
    private List<PlaceOrderItemRequest> items;

    // COD or ONLINE
    private PaymentMethod paymentMethod;
}
