package com.example.rapiffy.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * CartItem — one product (or variant) in the customer's cart.
 * shopProduct covers both plain products and variants since variant is now a first-class product.
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
