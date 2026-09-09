package com.example.rapiffy.impl;

import com.example.rapiffy.dto.admin.DeliveryPersonResponse;
import com.example.rapiffy.dto.invoice.InvoiceResponse;
import com.example.rapiffy.dto.order.OrderDetailResponse;
import com.example.rapiffy.dto.order.OrderItemResponse;
import com.example.rapiffy.dto.order.OrderSummaryResponse;
import com.example.rapiffy.enums.CancelledBy;
import com.example.rapiffy.enums.OrderStatus;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.services.NotificationService;
import com.example.rapiffy.impl.DeliveryPersonServiceImpl;
import com.example.rapiffy.model.*;
import com.example.rapiffy.repos.*;
import com.example.rapiffy.services.AdminOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminOrderServiceImpl implements AdminOrderService {

    private final OrderRepository orderRepository;
    private final ShopProductRepository shopProductRepository;
    private final ProfileRepository profileRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ParentOrderRepository parentOrderRepository;
    private final DeliveryPersonRepository deliveryPersonRepository;
    private final DeliveryPersonServiceImpl deliveryPersonService;
    private final NotificationService notificationService;

    public AdminOrderServiceImpl(OrderRepository orderRepository,
                                 ShopProductRepository shopProductRepository,
                                 ProfileRepository profileRepository,
                                 ProductVariantRepository productVariantRepository,
                                 ParentOrderRepository parentOrderRepository,
                                 DeliveryPersonRepository deliveryPersonRepository,
                                 DeliveryPersonServiceImpl deliveryPersonService,
                                 NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.shopProductRepository = shopProductRepository;
        this.profileRepository = profileRepository;
        this.productVariantRepository = productVariantRepository;
        this.parentOrderRepository = parentOrderRepository;
        this.deliveryPersonRepository = deliveryPersonRepository;
        this.deliveryPersonService = deliveryPersonService;
        this.notificationService = notificationService;
    }

    @Override
    public List<DeliveryPersonResponse> getDeliveryPersons(Long userId) {
        Profile shop = getShop(userId);
        return deliveryPersonRepository.findByShopId(shop.getId())
                .stream().map(deliveryPersonService::toResponse).collect(Collectors.toList());
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
    public ResponseEntity<OrderDetailResponse> updateOrderStatus(Long userId, Long orderId, OrderStatus status) {
        Profile shop = getShop(userId);
        Order order = getOrder(orderId, shop);

        switch (status) {
            case CONFIRMED -> {
                if (order.getStatus() != OrderStatus.PENDING) {
                    OrderDetailResponse detail = toDetail(order);
                    detail.setMessage("Order must be PENDING to confirm");
                    return ResponseEntity.badRequest().body(detail);
                }
                for (OrderItem item : order.getItems()) {
                    if (item.getVariantId() != null) {
                        productVariantRepository.findById(item.getVariantId()).ifPresent(variant -> {
                            if (variant.getStockQuantity() < item.getQuantity()) {
                                order.setStatus(OrderStatus.REJECTED);
                                order.setCancelledBy(CancelledBy.ADMIN);
                                order.setCancellationReason("Insufficient stock for: " + item.getProductName());
                                order.setCancelledAt(java.time.LocalDateTime.now());
                            } else {
                                variant.setStockQuantity(variant.getStockQuantity() - item.getQuantity());
                                productVariantRepository.save(variant);
                            }
                        });
                    } else {
                        ShopProduct sp = item.getShopProduct();
                        if (sp == null) continue;
                        if (sp.getStockQuantity() < item.getQuantity()) {
                            order.setStatus(OrderStatus.REJECTED);
                            order.setCancelledBy(CancelledBy.ADMIN);
                            order.setCancellationReason("Insufficient stock for: " + item.getProductName());
                            order.setCancelledAt(java.time.LocalDateTime.now());
                        } else {
                            sp.setStockQuantity(sp.getStockQuantity() - item.getQuantity());
                            shopProductRepository.save(sp);
                        }
                    }
                    if (order.getStatus() == OrderStatus.REJECTED) break;
                }

                if (order.getStatus() == OrderStatus.REJECTED) {
                    orderRepository.save(order);
                    syncParentOrderStatus(order);
                    return ResponseEntity.ok(toDetail(order));
                }
                if (order.getInvoiceId() == null)
                    order.setInvoiceId("INV-" + java.time.LocalDate.now().toString().replace("-", "") + "-" + String.format("%04d", order.getId()));
            }
            case READY -> {
                if (order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.DELIVERY_ACCEPTED) {
                    OrderDetailResponse detail = toDetail(order);
                    detail.setMessage("Order must be CONFIRMED or DELIVERY_ACCEPTED to mark READY");
                    return ResponseEntity.badRequest().body(detail);
                }
            }
            case OUT_FOR_DELIVERY -> {
                if (order.getStatus() != OrderStatus.COMING_TO_PICK) {
                    OrderDetailResponse detail = toDetail(order);
                    detail.setMessage("Order must be COMING_TO_PICK to mark OUT_FOR_DELIVERY");
                    return ResponseEntity.badRequest().body(detail);
                }
            }
            case DELIVERED -> {
                if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY) {
                    OrderDetailResponse detail = toDetail(order);
                    detail.setMessage("Order must be OUT_FOR_DELIVERY to mark DELIVERED");
                    return ResponseEntity.badRequest().body(detail);
                }
            }
            case REJECTED -> {
                if (order.getStatus() == OrderStatus.OUT_FOR_DELIVERY ||
                    order.getStatus() == OrderStatus.DELIVERED ||
                    order.getStatus() == OrderStatus.REJECTED ||
                    order.getStatus() == OrderStatus.CANCELLED) {
                    OrderDetailResponse detail = toDetail(order);
                    detail.setMessage("Order cannot be rejected at this stage");
                    return ResponseEntity.badRequest().body(detail);
                }
                order.setCancelledBy(CancelledBy.ADMIN);
                order.setCancellationReason("Rejected by shop");
                order.setCancelledAt(java.time.LocalDateTime.now());
            }
            default -> {
                OrderDetailResponse detail = toDetail(order);
                detail.setMessage("Invalid status: " + status);
                return ResponseEntity.badRequest().body(detail);
            }
        }

        order.setStatus(status);
        orderRepository.save(order);
        syncParentOrderStatus(order);

        // Notifications
        Long customerId = order.getCustomer().getId();
        Long deliveryPersonId = order.getDeliveryPerson() != null ? order.getDeliveryPerson().getId() : null;
        String orderNum = order.getOrderNumber();

        if (status == OrderStatus.CONFIRMED) {
            notificationService.send(customerId, "Order Confirmed", "Your order " + orderNum + " has been confirmed by the shop.", orderId, orderNum, status);
        } else if (status == OrderStatus.READY) {
            notificationService.send(customerId, "Order Ready", "Your order " + orderNum + " is packed and ready.", orderId, orderNum, status);
            if (deliveryPersonId != null)
                notificationService.send(deliveryPersonId, "Order Ready for Pickup", "Order " + orderNum + " is ready. Head to the shop.", orderId, orderNum, status);
        } else if (status == OrderStatus.OUT_FOR_DELIVERY) {
            notificationService.send(customerId, "Out for Delivery", "Your order " + orderNum + " is on the way!", orderId, orderNum, status);
            if (deliveryPersonId != null)
                notificationService.send(deliveryPersonId, "Out for Delivery", "Order " + orderNum + " marked out for delivery.", orderId, orderNum, status);
        } else if (status == OrderStatus.DELIVERED) {
            notificationService.send(customerId, "Order Delivered", "Your order " + orderNum + " has been delivered.", orderId, orderNum, status);
            if (deliveryPersonId != null)
                notificationService.send(deliveryPersonId, "Order Delivered", "Order " + orderNum + " marked as delivered.", orderId, orderNum, status);
        }

        return ResponseEntity.ok(toDetail(order));
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

    private void syncParentOrderStatus(Order updatedSubOrder) {
        ParentOrder parent = updatedSubOrder.getParentOrder();
        if (parent == null) return;

        List<Order> allSubOrders = orderRepository.findByParentOrder(parent);

        // Priority order — least advanced status wins
        List<OrderStatus> priority = List.of(
            OrderStatus.PAYMENT_PENDING,
            OrderStatus.PENDING,
            OrderStatus.CONFIRMED,
            OrderStatus.DELIVERY_ACCEPTED,
            OrderStatus.DELIVERY_REJECTED,
            OrderStatus.READY,
            OrderStatus.COMING_TO_PICK,
            OrderStatus.OUT_FOR_DELIVERY,
            OrderStatus.DELIVERED
        );

        OrderStatus lowestStatus = allSubOrders.stream()
            .map(Order::getStatus)
            .min(Comparator.comparingInt(priority::indexOf))
            .orElse(updatedSubOrder.getStatus());

        parent.setStatus(lowestStatus);
        parentOrderRepository.save(parent);
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
