package com.example.rapiffy.dto.customer.wallet;

import lombok.Data;

@Data
public class WalletTopupResponse {

    private String razorpayOrderId;
    private Long amount;       // in paise
    private String currency;
    private String razorpayKeyId;
}
