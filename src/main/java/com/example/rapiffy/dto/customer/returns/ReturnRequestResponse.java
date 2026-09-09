package com.example.rapiffy.dto.customer.returns;

import com.example.rapiffy.enums.ReturnReason;
import com.example.rapiffy.enums.ReturnStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReturnRequestResponse {

    private Long returnRequestId;
    private Long subOrderId;
    private String subOrderNumber;
    private String shopName;
    private ReturnReason reason;
    private ReturnStatus status;
    private String customerNote;
    private String adminNote;
    private Double refundAmount;
    private String refundMessage;  // Informational message about how/when refund will happen
    private List<ReturnItemResponse> items;
    private List<String> imageUrls;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
