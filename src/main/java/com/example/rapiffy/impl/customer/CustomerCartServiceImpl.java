package com.example.rapiffy.impl.customer;

import com.example.rapiffy.common.CAddress;
import com.example.rapiffy.dto.customer.*;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.*;
import com.example.rapiffy.repos.*;
import com.example.rapiffy.services.customer.CustomerCartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerCartServiceImpl implements CustomerCartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ShopProductRepository shopProductRepository;
    private final UserRepository userRepository;
    private final CustomerAddressRepository customerAddressRepository;

    @Override
    public CartResponse getCart(Long userId) {
        User customer = getUser(userId);
        Cart cart = cartRepository.findByCustomer(customer).orElse(null);
        if (cart == null || cart.getItems().isEmpty()) return emptyCart();
        return toCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addToCart(Long userId, AddToCartRequest request) {
        User customer = getUser(userId);

        ShopProduct sp = shopProductRepository.findById(request.getShopProductId())
                .orElseThrow(() -> new ApiException("Product not found", HttpStatus.NOT_FOUND));

        if (!sp.isActive())
            throw new ApiException("Product is not available: " + sp.getProductName(), HttpStatus.BAD_REQUEST);

        // ─── DELIVERY RANGE VALIDATION ───────────────────────────────────────
        Profile shop = sp.getShop();
        if (shop.getServingRangeInKm() != null && shop.getAddress() != null) {
            CustomerAddress defaultAddress = customerAddressRepository
                    .findByCustomerAndIsDefault(customer, true).orElse(null);
            if (defaultAddress != null && defaultAddress.getAddress() != null) {
                double distance = calculateDistanceKm(
                        shop.getAddress().getLatitude(), shop.getAddress().getLongitude(),
                        defaultAddress.getAddress().getLatitude(), defaultAddress.getAddress().getLongitude()
                );
                if (distance > shop.getServingRangeInKm())
                    throw new ApiException(
                            "This product is not deliverable to your saved address. "
                            + shop.getShopName() + " delivers within " + shop.getServingRangeInKm().intValue()
                            + " km, but your address is " + String.format("%.1f", distance) + " km away.",
                            HttpStatus.BAD_REQUEST);
            }
        }

        Cart cart = cartRepository.findByCustomer(customer).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setCustomer(customer);
            return cartRepository.save(newCart);
        });

        // Deduplicate: same shopProduct → increase qty
        Optional<CartItem> existing = cartItemRepository.findByCartAndShopProduct(cart, sp);
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            cartItemRepository.save(item);
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setShopProduct(sp);
            item.setQuantity(request.getQuantity());
            cartItemRepository.save(item);
        }

        return toCartResponse(cartRepository.findByCustomer(customer).get());
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(Long userId, Long cartItemId, UpdateCartItemRequest request) {
        User customer = getUser(userId);
        Cart cart = getCart(customer);
        CartItem item = getCartItem(cartItemId, cart);
        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);
        return toCartResponse(cartRepository.findByCustomer(customer).get());
    }

    @Override
    @Transactional
    public CartResponse removeCartItem(Long userId, Long cartItemId) {
        User customer = getUser(userId);
        Cart cart = getCart(customer);
        CartItem item = getCartItem(cartItemId, cart);
        cartItemRepository.delete(item);
        Cart updated = cartRepository.findByCustomer(customer).get();
        if (updated.getItems().isEmpty()) return emptyCart();
        return toCartResponse(updated);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        User customer = getUser(userId);
        cartRepository.findByCustomer(customer).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
        });
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
    }

    private Cart getCart(User customer) {
        return cartRepository.findByCustomer(customer)
                .orElseThrow(() -> new ApiException("Cart not found", HttpStatus.NOT_FOUND));
    }

    private CartItem getCartItem(Long cartItemId, Cart cart) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ApiException("Cart item not found", HttpStatus.NOT_FOUND));
        if (!item.getCart().getId().equals(cart.getId()))
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);
        return item;
    }

    private CartResponse emptyCart() {
        CartResponse r = new CartResponse();
        r.setTotalItems(0);
        r.setTotalAmount(0.0);
        r.setShops(new ArrayList<>());
        return r;
    }

    private CartResponse toCartResponse(Cart cart) {
        Map<Long, List<CartItem>> byShop = cart.getItems().stream()
                .collect(Collectors.groupingBy(i -> i.getShopProduct().getShop().getId()));

        List<CartShopGroup> shopGroups = new ArrayList<>();
        double grandTotal = 0.0;
        int totalItems = 0;

        for (Map.Entry<Long, List<CartItem>> entry : byShop.entrySet()) {
            Profile shop = entry.getValue().get(0).getShopProduct().getShop();
            List<CartItemResponse> itemResponses = new ArrayList<>();
            double shopTotal = 0.0;

            for (CartItem ci : entry.getValue()) {
                ShopProduct sp = ci.getShopProduct();
                double itemTotal = Math.round(sp.getSellingPrice() * ci.getQuantity() * 100.0) / 100.0;
                shopTotal += itemTotal;
                totalItems++;

                CartItemResponse ir = new CartItemResponse();
                ir.setCartItemId(ci.getId());
                ir.setShopProductId(sp.getId());
                ir.setProductName(sp.getProductName());
                ir.setBrand(sp.getBrand());
                ir.setUnit(sp.getUnit());
                ir.setUnitValue(sp.getUnitValue());
                ir.setImageUrl(sp.getImageUrl());
                ir.setMrp(sp.getMrp());
                ir.setSellingPrice(sp.getSellingPrice());
                ir.setQuantity(ci.getQuantity());
                ir.setItemTotal(itemTotal);
                itemResponses.add(ir);
            }

            CartShopGroup group = new CartShopGroup();
            group.setShopId(shop.getId());
            group.setShopName(shop.getShopName());
            group.setItems(itemResponses);
            group.setShopTotal(Math.round(shopTotal * 100.0) / 100.0);
            shopGroups.add(group);
            grandTotal += shopTotal;
        }

        CartResponse r = new CartResponse();
        r.setTotalItems(totalItems);
        r.setTotalAmount(Math.round(grandTotal * 100.0) / 100.0);
        r.setShops(shopGroups);
        return r;
    }

    private double calculateDistanceKm(String lat1Str, String lng1Str, String lat2Str, String lng2Str) {
        if (lat1Str == null || lng1Str == null || lat2Str == null || lng2Str == null) return 0.0;
        try {
            double lat1 = Double.parseDouble(lat1Str);
            double lng1 = Double.parseDouble(lng1Str);
            double lat2 = Double.parseDouble(lat2Str);
            double lng2 = Double.parseDouble(lng2Str);
            final double R = 6371;
            double dLat = Math.toRadians(lat2 - lat1);
            double dLng = Math.toRadians(lng2 - lng1);
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                    + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                    * Math.sin(dLng / 2) * Math.sin(dLng / 2);
            return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
