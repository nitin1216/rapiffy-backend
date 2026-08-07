package com.example.rapiffy.impl;

import com.example.rapiffy.dto.invoice.InvoiceResponse;
import com.example.rapiffy.dto.order.OrderDetailResponse;
import com.example.rapiffy.dto.order.OrderItemResponse;
import com.example.rapiffy.dto.order.OrderSummaryResponse;
import com.example.rapiffy.enums.OrderStatus;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.*;
import com.example.rapiffy.repos.*;
import com.example.rapiffy.services.AdminOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminOrderServiceImpl implements AdminOrderService {

    private final OrderRepository orderRepository;
    private final ShopProductRepository shopProductRepository;
    private final ProfileRepository profileRepository;

    public AdminOrderServiceImpl(OrderRepository orderRepository,
                                 ShopProductRepository shopProductRepository,
                                 ProfileRepository profileRepository) {
        this.orderRepository = orderRepository;
        this.shopProductRepository = shopProductRepository;
        this.profileRepository = profileRepository;
    }

    @Override
    public List<OrderSummaryResponse> getOrders(Long userId, OrderStatus status) {
        Profile shop = getShop(userId);
        List<Order> orders = status != null
            ? orderRepository.findByShopAndStatusOrderByCreatedAtDesc(shop, status)
            : orderRepository.findByShopOrderByCreatedAtDesc(shop);

        return orders.stream().map(this::toSummary).collect(Collectors.toList());
    }

    @Override
    public OrderDetailResponse getOrderDetail(Long userId, Long orderId) {
        Profile shop = getShop(userId);
        Order order = getOrder(orderId, shop);
        return toDetail(order);
    }

    @Override
    @Transactional
    public OrderDetailResponse confirmOrder(Long userId, Long orderId) {
        Profile shop = getShop(userId);
        Order order = getOrder(orderId, shop);

        if (order.getStatus() != OrderStatus.PENDING)
            throw new ApiException("Order is not in PENDING state", HttpStatus.BAD_REQUEST);

        // Deduct stock from product
        for (OrderItem item : order.getItems()) {
            ShopProduct sp = item.getShopProduct();
            if (sp == null) continue;
            if (sp.getStockQuantity() < item.getQuantity())
                throw new ApiException("Insufficient stock for: " + item.getProductName(), HttpStatus.BAD_REQUEST);
            sp.setStockQuantity(sp.getStockQuantity() - item.getQuantity());
            shopProductRepository.save(sp);
        }

        order.setStatus(OrderStatus.CONFIRMED);
        if (order.getInvoiceId() == null) {
            order.setInvoiceId("INV-" + java.time.LocalDate.now().toString().replace("-", "") + "-" + String.format("%04d", order.getId()));
        }
        return toDetail(orderRepository.save(order));
    }

    @Override
    public OrderDetailResponse markReady(Long userId, Long orderId) {
        Profile shop = getShop(userId);
        Order order = getOrder(orderId, shop);

        if (order.getStatus() != OrderStatus.CONFIRMED)
            throw new ApiException("Order must be CONFIRMED before marking READY", HttpStatus.BAD_REQUEST);

        order.setStatus(OrderStatus.READY);
        return toDetail(orderRepository.save(order));
    }

    @Override
    public OrderDetailResponse markOutForDelivery(Long userId, Long orderId) {
        Profile shop = getShop(userId);
        Order order = getOrder(orderId, shop);

        if (order.getStatus() != OrderStatus.READY)
            throw new ApiException("Order must be READY before marking OUT_FOR_DELIVERY", HttpStatus.BAD_REQUEST);

        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        return toDetail(orderRepository.save(order));
    }

    @Override
    public OrderDetailResponse markDelivered(Long userId, Long orderId) {
        Profile shop = getShop(userId);
        Order order = getOrder(orderId, shop);

        if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY)
            throw new ApiException("Order must be OUT_FOR_DELIVERY before marking DELIVERED", HttpStatus.BAD_REQUEST);

        order.setStatus(OrderStatus.DELIVERED);
        return toDetail(orderRepository.save(order));
    }

    @Override
    public InvoiceResponse getInvoice(Long userId, Long orderId) {
        Profile shop = getShop(userId);
        Order order = getOrder(orderId, shop);

        if (order.getInvoiceId() == null)
            throw new ApiException("Invoice not yet generated. Confirm the order first.", HttpStatus.BAD_REQUEST);

        InvoiceResponse r = new InvoiceResponse();
        r.setInvoiceId(order.getInvoiceId());
        r.setOrderNumber(order.getOrderNumber());
        r.setInvoiceDate(order.getUpdatedAt());

        // Shop details
        r.setShopName(shop.getShopName());
        r.setShopGstNumber(shop.getGstNumber());
        if (shop.getAddress() != null) {
            String addr = String.join(", ",
                nullSafe(shop.getAddress().getAddressLine1()),
                nullSafe(shop.getAddress().getCity()),
                nullSafe(shop.getAddress().getState()),
                nullSafe(shop.getAddress().getPinCode()));
            r.setShopAddress(addr);
        }
        if (shop.getPhoneNumber() != null)
            r.setShopPhone(shop.getPhoneNumber().getPhoneNumber());

        // Customer details
        r.setCustomerPhone(order.getCustomer().getPhoneNumber());
        r.setDeliveryAddress(order.getDeliveryAddress());
        r.setDeliveryType(order.getDeliveryType());

        // Totals
        r.setSubtotal(order.getSubtotal());
        r.setTotalGst(order.getTotalGst());
        r.setDeliveryCharge(order.getDeliveryCharge());
        r.setTotalAmount(order.getTotalAmount());

        // Items
        r.setItems(order.getItems().stream().map(item -> {
            OrderItemResponse i = new OrderItemResponse();
            i.setOrderItemId(item.getId());
            i.setShopProductId(item.getShopProduct() != null ? item.getShopProduct().getId() : null);
            i.setProductName(item.getProductName());
            i.setBrand(item.getBrand());
            i.setUnit(item.getUnit());
            i.setUnitValue(item.getUnitValue());
            i.setImageUrl(item.getImageUrl());
            i.setMrp(item.getMrp());
            i.setSellingPrice(item.getSellingPrice());
            i.setQuantity(item.getQuantity());
            i.setGstSlab(item.getGstSlab());
            i.setGstAmount(item.getGstAmount());
            i.setLineTotal(item.getLineTotal());
            return i;
        }).collect(Collectors.toList()));

        return r;
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    private Profile getShop(Long userId) {
        return profileRepository.findByUserId(userId)
            .orElseThrow(() -> new ApiException("Shop profile not found", HttpStatus.NOT_FOUND));
    }

    private Order getOrder(Long orderId, Profile shop) {
        return orderRepository.findByIdAndShop(orderId, shop)
            .orElseThrow(() -> new ApiException("Order not found", HttpStatus.NOT_FOUND));
    }

    private OrderSummaryResponse toSummary(Order order) {
        OrderSummaryResponse r = new OrderSummaryResponse();
        r.setOrderId(order.getId());
        r.setOrderNumber(order.getOrderNumber());
        r.setCustomerPhone(order.getCustomer().getPhoneNumber());
        r.setCustomerName(order.getCustomer().getPhoneNumber()); // name from profile if available
        r.setSubtotal(order.getSubtotal());
        r.setTotalGst(order.getTotalGst());
        r.setDeliveryCharge(order.getDeliveryCharge());
        r.setTotalAmount(order.getTotalAmount());
        r.setTotalItems(order.getItems().size());
        r.setStatus(order.getStatus());
        r.setDeliveryType(order.getDeliveryType());
        r.setCreatedAt(order.getCreatedAt());
        return r;
    }

    private OrderDetailResponse toDetail(Order order) {
        OrderDetailResponse r = new OrderDetailResponse();
        r.setOrderId(order.getId());
        r.setOrderNumber(order.getOrderNumber());
        r.setInvoiceId(order.getInvoiceId());
        r.setCustomerPhone(order.getCustomer().getPhoneNumber());
        r.setShopName(order.getShop().getShopName());
        r.setSubtotal(order.getSubtotal());
        r.setTotalGst(order.getTotalGst());
        r.setDeliveryCharge(order.getDeliveryCharge());
        r.setTotalAmount(order.getTotalAmount());
        r.setDeliveryType(order.getDeliveryType());
        r.setDeliveryAddress(order.getDeliveryAddress());
        r.setStatus(order.getStatus());
        r.setCreatedAt(order.getCreatedAt());
        r.setUpdatedAt(order.getUpdatedAt());

        List<OrderItemResponse> items = order.getItems().stream().map(item -> {
            OrderItemResponse i = new OrderItemResponse();
            i.setOrderItemId(item.getId());
            i.setShopProductId(item.getShopProduct() != null ? item.getShopProduct().getId() : null);
            i.setProductName(item.getProductName());
            i.setBrand(item.getBrand());
            i.setUnit(item.getUnit());
            i.setUnitValue(item.getUnitValue());
            i.setImageUrl(item.getImageUrl());
            i.setMrp(item.getMrp());
            i.setSellingPrice(item.getSellingPrice());
            i.setQuantity(item.getQuantity());
            i.setGstSlab(item.getGstSlab());
            i.setGstAmount(item.getGstAmount());
            i.setLineTotal(item.getLineTotal());
            return i;
        }).collect(Collectors.toList());

        r.setItems(items);
        return r;
    }
}
