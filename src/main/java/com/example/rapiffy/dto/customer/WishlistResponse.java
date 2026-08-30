package com.example.rapiffy.dto.customer;

import lombok.Data;

import java.util.List;

@Data
public class WishlistResponse {

    private Integer totalItems;
    private List<WishlistItemResponse> items;
}
