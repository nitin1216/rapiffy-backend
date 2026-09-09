package com.example.rapiffy.dto.customer.wallet;

import com.example.rapiffy.enums.WalletTransactionType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WalletTransactionResponse {

    private Long transactionId;
    private WalletTransactionType type;
    private String source;
    private Double amount;
    private String note;
    private LocalDateTime createdAt;
}
