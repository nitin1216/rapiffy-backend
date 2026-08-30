package com.example.rapiffy.controller.customer;

import com.example.rapiffy.dto.customer.CustomerProductResponse;
import com.example.rapiffy.dto.customer.WishlistResponse;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.User;
import com.example.rapiffy.repos.UserRepository;
import com.example.rapiffy.services.customer.CustomerWishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Customer - Wishlist", description = "Wishlist APIs. Login required.")
@RestController
@RequestMapping("/v1/customer/wishlist")
@RequiredArgsConstructor
public class CustomerWishlistController {

    private final CustomerWishlistService wishlistService;
    private final UserRepository userRepository;

    @Operation(summary = "Get my wishlist")
    @GetMapping
    public ResponseEntity<WishlistResponse> getWishlist() {
        return ResponseEntity.ok(wishlistService.getWishlist(getCurrentUserId()));
    }

    @Operation(
        summary = "Add product or variant to wishlist",
        description = "Pass shopProductId. If the product has variants, pass the variant's shopProductId. "
            + "If already wishlisted, it is silently ignored (no duplicates)."
    )
    @PostMapping("/add/{shopProductId}")
    public ResponseEntity<WishlistResponse> addToWishlist(@PathVariable Long shopProductId) {
        return ResponseEntity.ok(wishlistService.addToWishlist(getCurrentUserId(), shopProductId));
    }

    @Operation(summary = "Get full detail of a wishlisted item",
        description = "Returns full product details (with variants if applicable) for a wishlist item.")
    @GetMapping("/item/{wishlistItemId}")
    public ResponseEntity<CustomerProductResponse> getWishlistItemDetail(@PathVariable Long wishlistItemId) {
        return ResponseEntity.ok(wishlistService.getWishlistItemDetail(getCurrentUserId(), wishlistItemId));
    }

    @Operation(summary = "Remove item from wishlist")
    @DeleteMapping("/item/{wishlistItemId}")
    public ResponseEntity<WishlistResponse> removeFromWishlist(@PathVariable Long wishlistItemId) {
        return ResponseEntity.ok(wishlistService.removeFromWishlist(getCurrentUserId(), wishlistItemId));
    }

    @Operation(summary = "Clear entire wishlist")
    @DeleteMapping
    public ResponseEntity<Void> clearWishlist() {
        wishlistService.clearWishlist(getCurrentUserId());
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
