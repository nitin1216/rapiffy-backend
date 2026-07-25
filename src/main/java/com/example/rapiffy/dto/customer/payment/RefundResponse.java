package com.example.rapiffy.dto.customer.payment;

import com.example.rapiffy.enums.CancelledBy;
import com.example.rapiffy.enums.RefundStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Single refund entry — shown in refund history for a customer.
 */
@Data
public class RefundResponse {

    private Long refundId;

    // Which sub-order was cancelled
    private String subOrderNumber;
    private String shopName;

    // Refund details
    private Double amount;
    private RefundStatus status;
    private CancelledBy cancelledBy;
    private String reason;

    // Timestamps
    private LocalDateTime initiatedAt;
    private LocalDateTime completedAt;
}
