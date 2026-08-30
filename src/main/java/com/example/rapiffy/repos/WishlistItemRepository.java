package com.example.rapiffy.repos;

import com.example.rapiffy.model.ProductVariant;
import com.example.rapiffy.model.ShopProduct;
import com.example.rapiffy.model.Wishlist;
import com.example.rapiffy.model.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    Optional<WishlistItem> findByWishlistAndShopProduct(Wishlist wishlist, ShopProduct shopProduct);

    Optional<WishlistItem> findByWishlistAndProductVariant(Wishlist wishlist, ProductVariant productVariant);
}
