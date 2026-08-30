package com.example.rapiffy.dto.customer;

import lombok.Data;
import java.util.List;

@Data
public class CartPreviewShopGroup {

    private Long shopId;
    private String shopName;
    private List<CartPreviewItemResponse> items;
    private Double shopSubtotal;
    private Double shopGst;
    private Double shopTotal;
}
