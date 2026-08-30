package com.example.rapiffy.services.customer;

import com.example.rapiffy.dto.customer.CustomerProductResponse;
import com.example.rapiffy.dto.customer.WishlistResponse;

public interface CustomerWishlistService {

    WishlistResponse getWishlist(Long userId);

    WishlistResponse addToWishlist(Long userId, Long shopProductId);

    WishlistResponse removeFromWishlist(Long userId, Long wishlistItemId);

    void clearWishlist(Long userId);

    CustomerProductResponse getWishlistItemDetail(Long userId, Long wishlistItemId);
}
