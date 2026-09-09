package com.example.rapiffy.controller.delivery;

import com.example.rapiffy.dto.delivery.DeliveryOrderDetailResponse;
import com.example.rapiffy.dto.delivery.DeliveryOrderStatsResponse;
import com.example.rapiffy.dto.delivery.DeliveryOrderSummaryResponse;
import com.example.rapiffy.dto.delivery.DeliveryRouteResponse;
import com.example.rapiffy.dto.delivery.UpdateDeliveryStatusRequest;
import com.example.rapiffy.enums.OrderStatus;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.User;
import com.example.rapiffy.repos.UserRepository;
import com.example.rapiffy.dto.delivery.PushLocationRequest;
import com.example.rapiffy.services.delivery.DeliveryLocationService;
import com.example.rapiffy.services.delivery.DeliveryOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Delivery - Orders", description = "APIs for delivery person to view and update assigned orders. Login required.")
@RestController
@RequestMapping("/v1/delivery/orders")
@RequiredArgsConstructor
public class DeliveryOrderController {

    private final DeliveryOrderService deliveryOrderService;
    private final DeliveryLocationService deliveryLocationService;
    private final UserRepository userRepository;

    @Operation(
        summary = "Get my assigned orders",
        description = "Returns all orders assigned to the logged-in delivery person. Optionally filter by status e.g. ?status=OUT_FOR_DELIVERY"
    )
    @GetMapping
    public ResponseEntity<List<DeliveryOrderSummaryResponse>> getMyOrders(
            @RequestParam(required = false) OrderStatus status) {
        return ResponseEntity.ok(deliveryOrderService.getMyOrders(getCurrentUserId(), status));
    }

    @Operation(
        summary = "Get order detail",
        description = "Returns full order detail including items, shop info and delivery address. Only accessible if the order is assigned to this delivery person."
    )
    @GetMapping("/{orderId}")
    public ResponseEntity<DeliveryOrderDetailResponse> getOrderDetail(@PathVariable Long orderId) {
        return ResponseEntity.ok(deliveryOrderService.getOrderDetail(getCurrentUserId(), orderId));
    }

    @Operation(summary = "Get my order stats", description = "Returns active (OUT_FOR_DELIVERY), delivered, and total order counts.")
    @GetMapping("/stats")
    public ResponseEntity<DeliveryOrderStatsResponse> getMyStats() {
        return ResponseEntity.ok(deliveryOrderService.getMyStats(getCurrentUserId()));
    }

    @Operation(
        summary = "Get route for an order",
        description = "Returns shop (pickup) and customer (drop) coordinates for navigation. Use these waypoints with OSRM or any map SDK."
    )
    @GetMapping("/{orderId}/route")
    public ResponseEntity<DeliveryRouteResponse> getRoute(@PathVariable Long orderId) {
        return ResponseEntity.ok(deliveryOrderService.getRoute(getCurrentUserId(), orderId));
    }

    @Operation(
        summary = "Push live location",
        description = "Delivery person pushes their current GPS coordinates. Call every 5 sec while moving, 30 sec when stopped. Order must be OUT_FOR_DELIVERY."
    )
    @PostMapping("/location")
    public ResponseEntity<Void> pushLocation(@Valid @RequestBody PushLocationRequest request) {
        deliveryLocationService.pushLocation(getCurrentUserId(), request);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Update order status",
        description = "Delivery boy can update: CONFIRMED→DELIVERY_ACCEPTED, CONFIRMED→DELIVERY_REJECTED, READY→COMING_TO_PICK, COMING_TO_PICK→OUT_FOR_DELIVERY, OUT_FOR_DELIVERY→DELIVERED."
    )
    @PutMapping("/{orderId}/status")
    public ResponseEntity<DeliveryOrderDetailResponse> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateDeliveryStatusRequest request) {
        return ResponseEntity.ok(deliveryOrderService.updateOrderStatus(getCurrentUserId(), orderId, request));
    }

    private Long getCurrentUserId() {
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByPhoneNumber(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
        return user.getId();
    }
}
