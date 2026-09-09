package com.example.rapiffy.dto.customer.wallet;

import lombok.Data;

import java.util.List;

@Data
public class WalletResponse {

    private Double balance;
    private List<WalletTransactionResponse> transactions;
}
