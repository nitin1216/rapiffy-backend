package com.example.rapiffy.repos;

import com.example.rapiffy.model.Cart;
import com.example.rapiffy.model.CartItem;
import com.example.rapiffy.model.ProductVariant;
import com.example.rapiffy.model.ShopProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartAndShopProduct(Cart cart, ShopProduct shopProduct);

    Optional<CartItem> findByCartAndProductVariant(Cart cart, ProductVariant productVariant);
}
