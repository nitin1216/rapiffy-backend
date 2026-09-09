package com.example.rapiffy.impl.delivery;

import com.example.rapiffy.dto.delivery.DeliveryOrderDetailResponse;
import com.example.rapiffy.dto.delivery.DeliveryOrderStatsResponse;
import com.example.rapiffy.dto.delivery.DeliveryOrderSummaryResponse;
import com.example.rapiffy.dto.delivery.DeliveryRouteResponse;
import com.example.rapiffy.dto.delivery.UpdateDeliveryStatusRequest;
import com.example.rapiffy.dto.order.OrderItemResponse;
import com.example.rapiffy.enums.OrderStatus;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.Order;
import com.example.rapiffy.model.Profile;
import com.example.rapiffy.model.User;
import com.example.rapiffy.repos.OrderRepository;
import com.example.rapiffy.repos.UserRepository;
import com.example.rapiffy.services.NotificationService;
import com.example.rapiffy.services.delivery.DeliveryOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryOrderServiceImpl implements DeliveryOrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    public List<DeliveryOrderSummaryResponse> getMyOrders(Long deliveryUserId, OrderStatus status) {
        User deliveryPerson = getDeliveryUser(deliveryUserId);

        List<Order> orders = status != null
                ? orderRepository.findByDeliveryPersonAndStatusOrderByAssignedAtDesc(deliveryPerson, status)
                : orderRepository.findByDeliveryPersonOrderByAssignedAtDesc(deliveryPerson);

        return orders.stream().map(this::toSummary).toList();
    }

    @Override
    public DeliveryOrderDetailResponse getOrderDetail(Long deliveryUserId, Long orderId) {
        User deliveryPerson = getDeliveryUser(deliveryUserId);
        Order order = getOrder(orderId, deliveryPerson);
        return toDetail(order);
    }

    @Override
    @Transactional
    public DeliveryOrderDetailResponse updateOrderStatus(Long deliveryUserId, Long orderId, UpdateDeliveryStatusRequest request) {
        User deliveryPerson = getDeliveryUser(deliveryUserId);
        Order order = getOrder(orderId, deliveryPerson);

        switch (request.getStatus()) {
            case DELIVERY_ACCEPTED -> {
                if (order.getStatus() != OrderStatus.CONFIRMED)
                    throw new ApiException("Order must be CONFIRMED to accept", HttpStatus.BAD_REQUEST);
            }
            case DELIVERY_REJECTED -> {
                if (order.getStatus() != OrderStatus.CONFIRMED)
                    throw new ApiException("Order must be CONFIRMED to reject", HttpStatus.BAD_REQUEST);
            }
            case COMING_TO_PICK -> {
                if (order.getStatus() != OrderStatus.READY)
                    throw new ApiException("Order must be READY to mark COMING_TO_PICK", HttpStatus.BAD_REQUEST);
            }
            case OUT_FOR_DELIVERY -> {
                if (order.getStatus() != OrderStatus.COMING_TO_PICK)
                    throw new ApiException("Order must be COMING_TO_PICK to mark OUT_FOR_DELIVERY", HttpStatus.BAD_REQUEST);
            }
            case DELIVERED -> {
                if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY)
                    throw new ApiException("Order must be OUT_FOR_DELIVERY to mark DELIVERED", HttpStatus.BAD_REQUEST);
            }
            default -> throw new ApiException("Invalid status transition for delivery person", HttpStatus.BAD_REQUEST);
        }

        order.setStatus(request.getStatus());
        orderRepository.save(order);

        // Notifications
        String orderNum = order.getOrderNumber();
        Long oid = order.getId();
        Long customerId = order.getCustomer().getId();
        Long adminUserId = order.getShop().getUser() != null ? order.getShop().getUser().getId() : null;
        OrderStatus newStatus = request.getStatus();

        if (newStatus == OrderStatus.DELIVERY_ACCEPTED) {
            if (adminUserId != null)
                notificationService.send(adminUserId, "Delivery Accepted", "Delivery boy accepted order " + orderNum + ".", oid, orderNum, newStatus);
        } else if (newStatus == OrderStatus.DELIVERY_REJECTED) {
            if (adminUserId != null)
                notificationService.send(adminUserId, "Delivery Rejected", "Delivery boy rejected order " + orderNum + ". Please re-assign.", oid, orderNum, newStatus);
        } else if (newStatus == OrderStatus.COMING_TO_PICK) {
            if (adminUserId != null)
                notificationService.send(adminUserId, "Delivery Boy Coming", "Delivery boy is on the way to pick order " + orderNum + ".", oid, orderNum, newStatus);
        } else if (newStatus == OrderStatus.OUT_FOR_DELIVERY) {
            notificationService.send(customerId, "Out for Delivery", "Your order " + orderNum + " is on the way!", oid, orderNum, newStatus);
            if (adminUserId != null)
                notificationService.send(adminUserId, "Out for Delivery", "Order " + orderNum + " is out for delivery.", oid, orderNum, newStatus);
        } else if (newStatus == OrderStatus.DELIVERED) {
            notificationService.send(customerId, "Order Delivered", "Your order " + orderNum + " has been delivered.", oid, orderNum, newStatus);
            if (adminUserId != null)
                notificationService.send(adminUserId, "Order Delivered", "Order " + orderNum + " has been delivered.", oid, orderNum, newStatus);
        }

        return toDetail(order);
    }

    @Override
    public DeliveryOrderStatsResponse getMyStats(Long deliveryUserId) {
        User deliveryPerson = getDeliveryUser(deliveryUserId);
        long active = orderRepository.countByDeliveryPersonAndStatusIn(
                deliveryPerson, List.of(OrderStatus.DELIVERY_ACCEPTED, OrderStatus.READY, OrderStatus.COMING_TO_PICK, OrderStatus.OUT_FOR_DELIVERY));
        long delivered = orderRepository.countByDeliveryPersonAndStatus(
                deliveryPerson, OrderStatus.DELIVERED);
        Double totalEarned = orderRepository.sumDeliveryChargeByDeliveryPersonAndDelivered(deliveryPerson);
        return new DeliveryOrderStatsResponse(active, delivered, active + delivered, totalEarned);
    }

    @Override
    public DeliveryRouteResponse getRoute(Long deliveryUserId, Long orderId) {
        User deliveryPerson = getDeliveryUser(deliveryUserId);
        Order order = getOrder(orderId, deliveryPerson);
        Profile shop = order.getShop();
        String shopLat = shop.getAddress() != null ? shop.getAddress().getLatitude() : null;
        String shopLng = shop.getAddress() != null ? shop.getAddress().getLongitude() : null;
        String shopAddr = shop.getAddress() != null ? String.join(", ",
                nullSafe(shop.getAddress().getAddressLine1()),
                nullSafe(shop.getAddress().getCity()),
                nullSafe(shop.getAddress().getState()),
                nullSafe(shop.getAddress().getPinCode())) : null;
        return new DeliveryRouteResponse(
                shop.getShopName(), shopLat, shopLng, shopAddr,
                order.getDeliveryLatitude(), order.getDeliveryLongitude(), order.getDeliveryAddress());
    }

    private User getDeliveryUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
    }

    private Order getOrder(Long orderId, User deliveryPerson) {
        return orderRepository.findByIdAndDeliveryPerson(orderId, deliveryPerson)
                .orElseThrow(() -> new ApiException("Order not found or not assigned to you", HttpStatus.NOT_FOUND));
    }

    private DeliveryOrderSummaryResponse toSummary(Order order) {
        DeliveryOrderSummaryResponse r = new DeliveryOrderSummaryResponse();
        r.setOrderId(order.getId());
        r.setOrderNumber(order.getOrderNumber());
        r.setShopName(order.getShop().getShopName());
        r.setCustomerPhone(order.getCustomer().getPhoneNumber());
        r.setDeliveryAddress(order.getDeliveryAddress());
        r.setTotalAmount(order.getTotalAmount());
        r.setDeliveryCharge(order.getDeliveryCharge());
        r.setTotalItems(order.getItems().size());
        r.setStatus(order.getStatus());
        r.setAssignedAt(order.getAssignedAt());
        r.setCreatedAt(order.getCreatedAt());
        return r;
    }

    private DeliveryOrderDetailResponse toDetail(Order order) {
        DeliveryOrderDetailResponse r = new DeliveryOrderDetailResponse();
        r.setOrderId(order.getId());
        r.setOrderNumber(order.getOrderNumber());
        r.setCustomerPhone(order.getCustomer().getPhoneNumber());
        r.setDeliveryAddress(order.getDeliveryAddress());
        r.setTotalAmount(order.getTotalAmount());
        r.setDeliveryCharge(order.getDeliveryCharge());
        r.setStatus(order.getStatus());
        r.setAssignedAt(order.getAssignedAt());
        r.setCreatedAt(order.getCreatedAt());

        // Shop info
        Profile shop = order.getShop();
        r.setShopName(shop.getShopName());
        if (shop.getAddress() != null) {
            r.setShopAddress(String.join(", ",
                nullSafe(shop.getAddress().getAddressLine1()),
                nullSafe(shop.getAddress().getCity()),
                nullSafe(shop.getAddress().getState()),
                nullSafe(shop.getAddress().getPinCode())));
            r.setShopLatitude(shop.getAddress().getLatitude());
            r.setShopLongitude(shop.getAddress().getLongitude());
        }
        if (shop.getPhoneNumber() != null)
            r.setShopPhone(shop.getPhoneNumber().getPhoneNumber());

        r.setDeliveryLatitude(order.getDeliveryLatitude());
        r.setDeliveryLongitude(order.getDeliveryLongitude());

        // Items
        r.setItems(order.getItems().stream().map(item -> {
            OrderItemResponse i = new OrderItemResponse();
            i.setOrderItemId(item.getId());
            i.setProductName(item.getProductName());
            i.setBrand(item.getBrand());
            i.setImageUrl(item.getImageUrl());
            i.setQuantity(item.getQuantity());
            i.setSellingPrice(item.getSellingPrice());
            i.setLineTotal(item.getLineTotal());
            return i;
        }).toList());

        return r;
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}
