package com.example.rapiffy.impl.customer;

import com.example.rapiffy.common.CAddress;
import com.example.rapiffy.dto.customer.*;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.*;
import com.example.rapiffy.repos.*;
import com.example.rapiffy.services.customer.CustomerCartService;
import com.example.rapiffy.services.customer.CustomerOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CustomerCartServiceImpl implements CustomerCartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ShopProductRepository shopProductRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final CustomerOrderService customerOrderService;

    public CustomerCartServiceImpl(CartRepository cartRepository,
                                   CartItemRepository cartItemRepository,
                                   ShopProductRepository shopProductRepository,
                                   ProductVariantRepository productVariantRepository,
                                   UserRepository userRepository,
                                   CustomerAddressRepository customerAddressRepository,
                                   @Lazy CustomerOrderService customerOrderService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.shopProductRepository = shopProductRepository;
        this.productVariantRepository = productVariantRepository;
        this.userRepository = userRepository;
        this.customerAddressRepository = customerAddressRepository;
        this.customerOrderService = customerOrderService;
    }

    @Override
    @Transactional(readOnly = true)
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
        Long requestedId = request.getShopProductId();

        // Try ShopProduct first, then ProductVariant
        ShopProduct sp = shopProductRepository.findById(requestedId).orElse(null);
        ProductVariant variant = null;

        if (sp == null || sp.isHasVariants()) {
            // Either not found as ShopProduct, or it's a parent with variants
            // → must be a variant's shopProductId
            variant = productVariantRepository.findByShopProductId(requestedId)
                    .orElseThrow(() -> new ApiException("Product not found", HttpStatus.NOT_FOUND));
            if (!variant.isActive())
                throw new ApiException("Variant is not available: " + variant.getVariantName(), HttpStatus.BAD_REQUEST);
            sp = variant.getParentShopProduct();
        } else {
            if (!sp.isActive())
                throw new ApiException("Product is not available: " + sp.getProductName(), HttpStatus.BAD_REQUEST);
        }

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

        if (variant != null) {
            // Variant item — dedup by variant
            Optional<CartItem> existing = cartItemRepository.findByCartAndProductVariant(cart, variant);
            if (existing.isPresent()) {
                CartItem item = existing.get();
                item.setQuantity(item.getQuantity() + request.getQuantity());
                cartItemRepository.save(item);
            } else {
                CartItem item = new CartItem();
                item.setCart(cart);
                item.setProductVariant(variant);
                item.setQuantity(request.getQuantity());
                cartItemRepository.save(item);
            }
        } else {
            // Plain product — dedup by shopProduct
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
        cart.getItems().remove(item);
        cartRepository.save(cart);
        if (cart.getItems().isEmpty()) return emptyCart();
        return toCartResponse(cart);
    }

    @Override
    @Transactional
    public ParentOrderResponse checkoutFromCart(Long userId, CheckoutFromCartRequest request) {
        User customer = getUser(userId);
        Cart cart = getCart(customer);

        List<CartItem> selectedItems = request.getCartItemIds().stream()
                .map(id -> {
                    CartItem item = cartItemRepository.findById(id)
                            .orElseThrow(() -> new ApiException("Cart item not found: " + id, HttpStatus.NOT_FOUND));
                    if (!item.getCart().getId().equals(cart.getId()))
                        throw new ApiException("Access denied for cart item: " + id, HttpStatus.FORBIDDEN);
                    return item;
                })
                .toList();

        List<PlaceOrderItemRequest> items = selectedItems.stream()
                .map(ci -> {
                    PlaceOrderItemRequest item = new PlaceOrderItemRequest();
                    if (ci.getProductVariant() != null)
                        item.setShopProductId(ci.getProductVariant().getShopProductId());
                    else
                        item.setShopProductId(ci.getShopProduct().getId());
                    item.setQuantity(ci.getQuantity());
                    return item;
                })
                .toList();

        // Resolve delivery address
        String deliveryAddress = null;
        if (request.getAddressId() != null) {
            CustomerAddress address = customerAddressRepository.findById(request.getAddressId())
                    .orElseThrow(() -> new ApiException("Address not found", HttpStatus.NOT_FOUND));
            if (!address.getCustomer().getId().equals(userId))
                throw new ApiException("Access denied for this address", HttpStatus.FORBIDDEN);
            deliveryAddress = buildAddressString(address);
        } else {
            CustomerAddress address = customerAddressRepository.findByCustomerAndIsDefault(customer, true)
                    .orElseThrow(() -> new ApiException(
                            "No address selected and no default address saved.", HttpStatus.BAD_REQUEST));
            deliveryAddress = buildAddressString(address);
        }

        PlaceOrderRequest orderRequest = new PlaceOrderRequest();
        orderRequest.setDeliveryType("DELIVERY");
        orderRequest.setDeliveryAddress(deliveryAddress);
        orderRequest.setDeliveryInstruction(request.getDeliveryInstruction());
        orderRequest.setItems(items);
        orderRequest.setPaymentMethod(request.getPaymentMethod());

        cartItemRepository.deleteAll(selectedItems);

        return customerOrderService.placeOrder(userId, orderRequest);
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

    @Override
    public CartPreviewResponse previewCart(Long userId, List<Long> cartItemIds) {
        User customer = getUser(userId);
        Cart cart = getCart(customer);

        // Group resolved items by shopId
        Map<Long, List<CartPreviewItemResponse>> byShop = new LinkedHashMap<>();
        Map<Long, Profile> shopById = new LinkedHashMap<>();
        double subtotal = 0.0;
        double totalGst = 0.0;
        int totalItems = 0;

        for (Long cartItemId : cartItemIds) {
            CartItem ci = getCartItem(cartItemId, cart);

            CartPreviewItemResponse item = new CartPreviewItemResponse();
            item.setCartItemId(ci.getId());
            item.setQuantity(ci.getQuantity());

            Profile shop;
            double lineSubtotal;
            double gstAmount;

            if (ci.getProductVariant() != null) {
                ProductVariant v = ci.getProductVariant();
                ShopProduct parent = v.getParentShopProduct();
                shop = parent.getShop();

                item.setShopProductId(v.getShopProductId());
                item.setShopId(shop.getId());
                item.setShopName(shop.getShopName());
                item.setProductName(v.getVariantName());
                item.setBrand(v.getBrand());
                item.setUnit(parent.getUnit());
                item.setUnitValue(parent.getUnitValue());
                item.setShortDescription(v.getShortDescription());
                item.setLongDescription(v.getLongDescription());
                item.setImageUrl(v.getImageUrl());
                item.setImageGallery(v.getImages().stream().map(ProductVariantImage::getImageUrl).toList());
                item.setMrp(v.getMrp());
                item.setSellingPrice(v.getSellingPrice());
                item.setGstSlab(v.getGstSlab());
                item.setStockQuantity(v.getStockQuantity());
                item.setExpiryDate(v.getExpiryDate());

                lineSubtotal = v.getSellingPrice() * ci.getQuantity();
                gstAmount = Math.round(lineSubtotal * parseGstRate(v.getGstSlab()) * 100.0) / 100.0;
                item.setDiscountPercent(v.getMrp() != null && v.getMrp() > 0
                        ? Math.round((v.getMrp() - v.getSellingPrice()) / v.getMrp() * 100.0 * 10.0) / 10.0
                        : 0.0);
            } else {
                ShopProduct sp = ci.getShopProduct();
                shop = sp.getShop();

                item.setShopProductId(sp.getId());
                item.setShopId(shop.getId());
                item.setShopName(shop.getShopName());
                item.setProductName(sp.getProductName());
                item.setBrand(sp.getBrand());
                item.setUnit(sp.getUnit());
                item.setUnitValue(sp.getUnitValue());
                item.setShortDescription(sp.getShortDescription());
                item.setLongDescription(sp.getLongDescription());
                item.setImageUrl(sp.getImageUrl());
                item.setImageGallery(sp.getImages().stream().map(ShopProductImage::getImageUrl).toList());
                item.setMrp(sp.getMrp());
                item.setSellingPrice(sp.getSellingPrice());
                item.setGstSlab(sp.getGstSlab());
                item.setStockQuantity(sp.getStockQuantity());
                item.setExpiryDate(sp.getExpiryDate());

                lineSubtotal = sp.getSellingPrice() * ci.getQuantity();
                gstAmount = Math.round(lineSubtotal * parseGstRate(sp.getGstSlab()) * 100.0) / 100.0;
                item.setDiscountPercent(sp.getMrp() != null && sp.getMrp() > 0
                        ? Math.round((sp.getMrp() - sp.getSellingPrice()) / sp.getMrp() * 100.0 * 10.0) / 10.0
                        : 0.0);
            }

            item.setGstAmount(gstAmount);
            item.setItemTotal(Math.round((lineSubtotal + gstAmount) * 100.0) / 100.0);
            subtotal += lineSubtotal;
            totalGst += gstAmount;
            totalItems++;

            shopById.putIfAbsent(shop.getId(), shop);
            byShop.computeIfAbsent(shop.getId(), k -> new ArrayList<>()).add(item);
        }

        CartPreviewResponse response = new CartPreviewResponse();
        response.setTotalItems(totalItems);
        response.setSubtotal(Math.round(subtotal * 100.0) / 100.0);
        response.setTotalGst(Math.round(totalGst * 100.0) / 100.0);
        response.setGrandTotal(Math.round((subtotal + totalGst) * 100.0) / 100.0);

        if (byShop.size() == 1) {
            response.setMultiShop(false);
            response.setItems(byShop.values().iterator().next());
        } else {
            response.setMultiShop(true);
            List<CartPreviewShopGroup> shops = new ArrayList<>();
            for (Map.Entry<Long, List<CartPreviewItemResponse>> entry : byShop.entrySet()) {
                Profile shop = shopById.get(entry.getKey());
                List<CartPreviewItemResponse> shopItems = entry.getValue();

                double shopSubtotal = shopItems.stream().mapToDouble(i -> i.getSellingPrice() * i.getQuantity()).sum();
                double shopGst = shopItems.stream().mapToDouble(CartPreviewItemResponse::getGstAmount).sum();

                CartPreviewShopGroup group = new CartPreviewShopGroup();
                group.setShopId(shop.getId());
                group.setShopName(shop.getShopName());
                group.setItems(shopItems);
                group.setShopSubtotal(Math.round(shopSubtotal * 100.0) / 100.0);
                group.setShopGst(Math.round(shopGst * 100.0) / 100.0);
                group.setShopTotal(Math.round((shopSubtotal + shopGst) * 100.0) / 100.0);
                shops.add(group);
            }
            response.setShops(shops);
        }

        return response;
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
                .collect(Collectors.groupingBy(i -> {
                    if (i.getProductVariant() != null)
                        return i.getProductVariant().getParentShopProduct().getShop().getId();
                    return i.getShopProduct().getShop().getId();
                }));

        List<CartShopGroup> shopGroups = new ArrayList<>();
        double grandTotal = 0.0;
        int totalItems = 0;

        for (Map.Entry<Long, List<CartItem>> entry : byShop.entrySet()) {
            CartItem first = entry.getValue().get(0);
            Profile shop = first.getProductVariant() != null
                    ? first.getProductVariant().getParentShopProduct().getShop()
                    : first.getShopProduct().getShop();
            List<CartItemResponse> itemResponses = new ArrayList<>();
            double shopTotal = 0.0;

            for (CartItem ci : entry.getValue()) {
                double sellingPrice;
                CartItemResponse ir = new CartItemResponse();
                ir.setCartItemId(ci.getId());
                ir.setQuantity(ci.getQuantity());

                if (ci.getProductVariant() != null) {
                    ProductVariant v = ci.getProductVariant();
                    sellingPrice = v.getSellingPrice();
                    ir.setShopProductId(v.getShopProductId());
                    ir.setProductName(v.getVariantName());
                    ir.setBrand(v.getBrand());
                    ir.setUnit(v.getParentShopProduct().getUnit());
                    ir.setUnitValue(v.getParentShopProduct().getUnitValue());
                    ir.setImageUrl(v.getImageUrl());
                    ir.setMrp(v.getMrp());
                    ir.setSellingPrice(sellingPrice);
                } else {
                    ShopProduct sp = ci.getShopProduct();
                    sellingPrice = sp.getSellingPrice();
                    ir.setShopProductId(sp.getId());
                    ir.setProductName(sp.getProductName());
                    ir.setBrand(sp.getBrand());
                    ir.setUnit(sp.getUnit());
                    ir.setUnitValue(sp.getUnitValue());
                    ir.setImageUrl(sp.getImageUrl());
                    ir.setMrp(sp.getMrp());
                    ir.setSellingPrice(sellingPrice);
                }

                double itemTotal = Math.round(sellingPrice * ci.getQuantity() * 100.0) / 100.0;
                shopTotal += itemTotal;
                totalItems++;
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

    private String buildAddressString(CustomerAddress address) {
        return String.join(", ",
                nullSafe(address.getAddress().getAddressLine1()),
                nullSafe(address.getAddress().getCity()),
                nullSafe(address.getAddress().getState()),
                nullSafe(address.getAddress().getPinCode()));
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
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

    private double parseGstRate(String gstSlab) {
        if (gstSlab == null || gstSlab.isBlank()) return 0.0;
        try {
            return Double.parseDouble(gstSlab.replace("%", "").trim()) / 100.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
