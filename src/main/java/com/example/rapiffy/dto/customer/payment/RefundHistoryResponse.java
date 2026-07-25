package com.example.rapiffy.dto.customer.payment;

import lombok.Data;

import java.util.List;

/**
 * Response showing all refunds for a parent order.
 * Customer can see which sub-orders were refunded and their status.
 */
@Data
public class RefundHistoryResponse {

    private Long parentOrderId;
    private String orderNumber;
    private Double totalPaid;
    private Double totalRefunded;
    private List<RefundResponse> refunds;
}
