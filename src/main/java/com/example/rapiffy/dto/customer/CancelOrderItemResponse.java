package com.example.rapiffy.dto.customer;

import com.example.rapiffy.enums.CancellationStatus;
import com.example.rapiffy.enums.OrderStatus;
import lombok.Data;

@Data
public class CancelOrderItemResponse {

    private Long cancellationRequestId;
    private Long subOrderId;
    private String subOrderNumber;
    private String shopName;
    private OrderStatus subOrderStatus;
    private CancellationStatus cancellationStatus;
    private Double refundAmount;    // amount that will be refunded on approval
    private String message;         // tells customer what happens next
}
