package com.example.rapiffy.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * CartItem — one product in the customer's cart.
 * Linked to ShopProduct so we always have latest price/stock info.
 */
@Entity
@Table(name = "cart_items")
@Data
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_product_id", nullable = false)
    private ShopProduct shopProduct;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;
}
