package com.example.rapiffy.dto.customer;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class CartPreviewItemResponse {

    private Long cartItemId;
    private Long shopProductId;
    private Long shopId;
    private String shopName;

    private String productName;
    private String brand;
    private String unit;
    private String unitValue;
    private String shortDescription;
    private String longDescription;

    private String imageUrl;
    private List<String> imageGallery;

    private Double mrp;
    private Double sellingPrice;
    private Double discountPercent;
    private String gstSlab;
    private Double gstAmount;

    private Integer stockQuantity;
    private LocalDate expiryDate;

    private Integer quantity;
    private Double itemTotal;
}
