package com.example.rapiffy.dto.superadmin;

import lombok.Data;

@Data
public class AddProductToShopRequest {

    private String adminPhone;
    private Long masterProductId;
    private Long categoryId;
}
