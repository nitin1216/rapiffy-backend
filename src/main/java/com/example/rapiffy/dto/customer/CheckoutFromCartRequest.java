package com.example.rapiffy.dto.customer;

import com.example.rapiffy.enums.PaymentMethod;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CheckoutFromCartRequest {

    @NotEmpty(message = "Select at least one item to checkout")
    private List<Long> cartItemIds;

    // If not passed → backend uses default saved address
    private Long addressId;

    // Optional delivery instruction for the delivery person
    private String deliveryInstruction;

    // COD or ONLINE — defaults to COD if not passed
    @NotNull(message = "paymentMethod is required")
    private PaymentMethod paymentMethod;
}
