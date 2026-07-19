package com.example.rapiffy.dto.customer;

import lombok.Data;

import java.util.List;

@Data
public class CartShopGroup {

    private Long shopId;
    private String shopName;
    private List<CartItemResponse> items;
    private Double shopTotal; // sum of all item totals in this shop
}
