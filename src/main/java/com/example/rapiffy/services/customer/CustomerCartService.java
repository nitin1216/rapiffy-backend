package com.example.rapiffy.services.customer;

import com.example.rapiffy.dto.customer.AddToCartRequest;
import com.example.rapiffy.dto.customer.CartResponse;
import com.example.rapiffy.dto.customer.UpdateCartItemRequest;

public interface CustomerCartService {

    CartResponse getCart(Long userId);

    CartResponse addToCart(Long userId, AddToCartRequest request);

    CartResponse updateCartItem(Long userId, Long cartItemId, UpdateCartItemRequest request);

    CartResponse removeCartItem(Long userId, Long cartItemId);

    void clearCart(Long userId);
}
