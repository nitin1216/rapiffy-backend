package com.example.rapiffy.controller.customer;

import com.example.rapiffy.dto.customer.AddToCartRequest;
import com.example.rapiffy.dto.customer.CartPreviewResponse;
import com.example.rapiffy.dto.customer.CartResponse;
import com.example.rapiffy.dto.customer.CheckoutFromCartRequest;
import com.example.rapiffy.dto.customer.ParentOrderResponse;
import com.example.rapiffy.dto.customer.UpdateCartItemRequest;
import java.util.List;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.User;
import com.example.rapiffy.repos.UserRepository;
import com.example.rapiffy.services.customer.CustomerCartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Customer - Cart", description = "Cart management APIs. Login required.")
@RestController
@RequestMapping("/v1/customer/cart")
@RequiredArgsConstructor
public class CustomerCartController {

    private final CustomerCartService cartService;
    private final UserRepository userRepository;

    @Operation(
        summary = "Get my cart",
        description = "Returns current cart grouped by shop with shop-level and grand totals."
    )
    @GetMapping
    public ResponseEntity<CartResponse> getCart() {
        return ResponseEntity.ok(cartService.getCart(getCurrentUserId()));
    }

    @Operation(
        summary = "Add item to cart",
        description = "Adds a product to cart. If same product already exists, quantity is increased. "
            + "Validates stock availability before adding."
    )
    @PostMapping("/add")
    public ResponseEntity<CartResponse> addToCart(@Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(cartService.addToCart(getCurrentUserId(), request));
    }

    @Operation(
        summary = "Update cart item quantity",
        description = "Updates quantity of a specific cart item. Validates stock availability."
    )
    @PutMapping("/item/{cartItemId}")
    public ResponseEntity<CartResponse> updateCartItem(
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(cartService.updateCartItem(getCurrentUserId(), cartItemId, request));
    }

    @Operation(
        summary = "Remove item from cart",
        description = "Removes a specific item from cart. Returns updated cart."
    )
    @DeleteMapping("/item/{cartItemId}")
    public ResponseEntity<CartResponse> removeCartItem(@PathVariable Long cartItemId) {
        return ResponseEntity.ok(cartService.removeCartItem(getCurrentUserId(), cartItemId));
    }

    @Operation(
        summary = "Preview selected cart items",
        description = "Returns full product details and price summary for selected cart items."
    )
    @PostMapping("/preview")
    public ResponseEntity<CartPreviewResponse> previewCart(@RequestBody List<Long> cartItemIds) {
        return ResponseEntity.ok(cartService.previewCart(getCurrentUserId(), cartItemIds));
    }

    @Operation(
        summary = "Checkout selected cart items",
        description = "Places an order using only the selected cart items (by cartItemId). "
            + "Unselected items remain in cart. Works for select-one, select-all, or any combination."
    )
    @PostMapping("/checkout")
    public ResponseEntity<ParentOrderResponse> checkoutFromCart(@Valid @RequestBody CheckoutFromCartRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartService.checkoutFromCart(getCurrentUserId(), request));
    }

    @Operation(
        description = "Removes all items from cart. Called automatically after order is placed."
    )
    @DeleteMapping
    public ResponseEntity<Void> clearCart() {
        cartService.clearCart(getCurrentUserId());
        return ResponseEntity.ok().build();
    }

    private Long getCurrentUserId() {
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByPhoneNumber(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
        return user.getId();
    }
}
