package com.example.rapiffy.dto.customer;

import lombok.Data;

import java.util.List;

@Data
public class CartResponse {

    private Integer totalItems;
    private Double totalAmount; // grand total across all shops
    private List<CartShopGroup> shops;
}
