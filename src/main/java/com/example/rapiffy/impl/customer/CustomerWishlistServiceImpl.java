package com.example.rapiffy.impl.customer;

import com.example.rapiffy.dto.customer.CustomerProductResponse;
import com.example.rapiffy.dto.customer.WishlistItemResponse;
import com.example.rapiffy.dto.customer.WishlistResponse;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.*;
import com.example.rapiffy.repos.*;
import com.example.rapiffy.services.customer.CustomerShopService;
import com.example.rapiffy.services.customer.CustomerWishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerWishlistServiceImpl implements CustomerWishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final ShopProductRepository shopProductRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final CustomerShopService customerShopService;

    @Override
    public WishlistResponse getWishlist(Long userId) {
        User customer = getUser(userId);
        Wishlist wishlist = wishlistRepository.findByCustomer(customer).orElse(null);
        if (wishlist == null || wishlist.getItems().isEmpty()) return emptyWishlist();
        return toWishlistResponse(wishlist);
    }

    @Override
    @Transactional
    public WishlistResponse addToWishlist(Long userId, Long shopProductId) {
        User customer = getUser(userId);

        // Same resolution logic as cart: try ShopProduct first, then ProductVariant
        ShopProduct sp = shopProductRepository.findById(shopProductId).orElse(null);
        ProductVariant variant = null;

        if (sp == null || sp.isHasVariants()) {
            variant = productVariantRepository.findByShopProductId(shopProductId)
                    .orElseThrow(() -> new ApiException("Product not found", HttpStatus.NOT_FOUND));
            if (!variant.isActive())
                throw new ApiException("Variant is not available: " + variant.getVariantName(), HttpStatus.BAD_REQUEST);
            sp = variant.getParentShopProduct();
        } else {
            if (!sp.isActive())
                throw new ApiException("Product is not available: " + sp.getProductName(), HttpStatus.BAD_REQUEST);
        }

        Wishlist wishlist = wishlistRepository.findByCustomer(customer).orElseGet(() -> {
            Wishlist w = new Wishlist();
            w.setCustomer(customer);
            return wishlistRepository.save(w);
        });

        if (variant != null) {
            // Already wishlisted? skip duplicate
            if (wishlistItemRepository.findByWishlistAndProductVariant(wishlist, variant).isEmpty()) {
                WishlistItem item = new WishlistItem();
                item.setWishlist(wishlist);
                item.setProductVariant(variant);
                wishlistItemRepository.save(item);
            }
        } else {
            if (wishlistItemRepository.findByWishlistAndShopProduct(wishlist, sp).isEmpty()) {
                WishlistItem item = new WishlistItem();
                item.setWishlist(wishlist);
                item.setShopProduct(sp);
                wishlistItemRepository.save(item);
            }
        }

        return toWishlistResponse(wishlistRepository.findByCustomer(customer).get());
    }

    @Override
    @Transactional
    public WishlistResponse removeFromWishlist(Long userId, Long wishlistItemId) {
        User customer = getUser(userId);
        Wishlist wishlist = wishlistRepository.findByCustomer(customer)
                .orElseThrow(() -> new ApiException("Wishlist not found", HttpStatus.NOT_FOUND));
        WishlistItem item = wishlistItemRepository.findById(wishlistItemId)
                .orElseThrow(() -> new ApiException("Wishlist item not found", HttpStatus.NOT_FOUND));
        if (!item.getWishlist().getId().equals(wishlist.getId()))
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);
        wishlist.getItems().remove(item);
        wishlistRepository.save(wishlist);
        if (wishlist.getItems().isEmpty()) return emptyWishlist();
        return toWishlistResponse(wishlist);
    }

    @Override
    @Transactional
    public void clearWishlist(Long userId) {
        User customer = getUser(userId);
        wishlistRepository.findByCustomer(customer).ifPresent(w -> {
            w.getItems().clear();
            wishlistRepository.save(w);
        });
    }

    @Override
    public CustomerProductResponse getWishlistItemDetail(Long userId, Long wishlistItemId) {
        User customer = getUser(userId);
        Wishlist wishlist = wishlistRepository.findByCustomer(customer)
                .orElseThrow(() -> new ApiException("Wishlist not found", HttpStatus.NOT_FOUND));
        WishlistItem item = wishlistItemRepository.findById(wishlistItemId)
                .orElseThrow(() -> new ApiException("Wishlist item not found", HttpStatus.NOT_FOUND));
        if (!item.getWishlist().getId().equals(wishlist.getId()))
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);

        // Resolve the shopProductId to pass to existing getProductById
        Long shopProductId = item.getProductVariant() != null
                ? item.getProductVariant().getParentShopProduct().getId()  // return parent so full variants load
                : item.getShopProduct().getId();

        return customerShopService.getProductById(shopProductId);
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
    }

    private WishlistResponse emptyWishlist() {
        WishlistResponse r = new WishlistResponse();
        r.setTotalItems(0);
        r.setItems(new ArrayList<>());
        return r;
    }

    private WishlistResponse toWishlistResponse(Wishlist wishlist) {
        List<WishlistItemResponse> itemResponses = new ArrayList<>();
        for (WishlistItem wi : wishlist.getItems()) {
            WishlistItemResponse ir = new WishlistItemResponse();
            ir.setWishlistItemId(wi.getId());
            if (wi.getProductVariant() != null) {
                ProductVariant v = wi.getProductVariant();
                ir.setShopProductId(v.getShopProductId());
                ir.setProductName(v.getVariantName());
                ir.setBrand(v.getBrand());
                ir.setUnit(v.getParentShopProduct().getUnit());
                ir.setUnitValue(v.getParentShopProduct().getUnitValue());
                ir.setImageUrl(v.getImageUrl());
                ir.setMrp(v.getMrp());
                ir.setSellingPrice(v.getSellingPrice());
                ir.setShopId(v.getParentShopProduct().getShop().getId());
                ir.setShopName(v.getParentShopProduct().getShop().getShopName());
            } else {
                ShopProduct sp = wi.getShopProduct();
                ir.setShopProductId(sp.getId());
                ir.setProductName(sp.getProductName());
                ir.setBrand(sp.getBrand());
                ir.setUnit(sp.getUnit());
                ir.setUnitValue(sp.getUnitValue());
                ir.setImageUrl(sp.getImageUrl());
                ir.setMrp(sp.getMrp());
                ir.setSellingPrice(sp.getSellingPrice());
                ir.setShopId(sp.getShop().getId());
                ir.setShopName(sp.getShop().getShopName());
            }
            itemResponses.add(ir);
        }
        WishlistResponse r = new WishlistResponse();
        r.setTotalItems(itemResponses.size());
        r.setItems(itemResponses);
        return r;
    }
}
