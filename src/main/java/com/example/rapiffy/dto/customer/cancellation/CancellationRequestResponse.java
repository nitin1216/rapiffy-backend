package com.example.rapiffy.dto.customer.cancellation;

import com.example.rapiffy.enums.CancellationStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CancellationRequestResponse {
    private Long cancellationRequestId;
    private Long subOrderId;
    private String subOrderNumber;
    private String shopName;
    private String reason;
    private String adminNote;
    private CancellationStatus status;
    private Double refundAmount;
    private List<CancellationItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
