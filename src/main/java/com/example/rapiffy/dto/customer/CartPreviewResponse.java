package com.example.rapiffy.dto.customer;

import lombok.Data;
import java.util.List;

@Data
public class CartPreviewResponse {

    private boolean multiShop;
    private List<CartPreviewItemResponse> items;  // populated when multiShop=false
    private List<CartPreviewShopGroup> shops;      // populated when multiShop=true
    private Integer totalItems;
    private Double subtotal;
    private Double totalGst;
    private Double grandTotal;
}
