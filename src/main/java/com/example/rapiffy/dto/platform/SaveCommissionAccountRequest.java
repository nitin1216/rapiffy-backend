package com.example.rapiffy.dto.platform;

import lombok.Data;

@Data
public class SaveCommissionAccountRequest {
    private String businessName;
    private String businessType;   // individual, partnership, etc.
    private String email;
    private String bankAccountNumber;
    private String ifsc;
    private String beneficiaryName;
}
