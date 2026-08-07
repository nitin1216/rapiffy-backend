package com.example.rapiffy.dto.superadmin;

import lombok.Data;

@Data
public class LinkShopRazorpayRequest {
    private String phoneNumber;       // Admin's phone to identify the shop
    private String businessName;      // Legal business name
    private String businessType;      // route, individual, etc.
    private String email;
    private String profile;           // "payments" or "route"
    private String bankAccountNumber;
    private String ifsc;
    private String beneficiaryName;
}
