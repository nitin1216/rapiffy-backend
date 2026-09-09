package com.example.rapiffy.impl.delivery;

import com.example.rapiffy.dto.delivery.DeliveryLocationResponse;
import com.example.rapiffy.dto.delivery.PushLocationRequest;
import com.example.rapiffy.enums.OrderStatus;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.DeliveryLocation;
import com.example.rapiffy.model.Order;
import com.example.rapiffy.repos.DeliveryLocationRepository;
import com.example.rapiffy.repos.OrderRepository;
import com.example.rapiffy.repos.UserRepository;
import com.example.rapiffy.services.delivery.DeliveryLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
//import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class DeliveryLocationServiceImpl implements DeliveryLocationService {

    private final DeliveryLocationRepository deliveryLocationRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void pushLocation(Long deliveryUserId, PushLocationRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ApiException("Order not found", HttpStatus.NOT_FOUND));

        // Only the assigned delivery person can push location for this order
        if (order.getDeliveryPerson() == null ||
                !order.getDeliveryPerson().getId().equals(deliveryUserId))
            throw new ApiException("You are not assigned to this order", HttpStatus.FORBIDDEN);

        // Only track when order is OUT_FOR_DELIVERY
        if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY)
            throw new ApiException("Location tracking is only active when order is OUT_FOR_DELIVERY", HttpStatus.BAD_REQUEST);

        // Upsert — update existing record or create new one
        DeliveryLocation location = deliveryLocationRepository.findByOrderId(order.getId())
                .orElse(new DeliveryLocation());

        location.setOrder(order);
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setAccuracy(request.getAccuracy());
        location.setBatteryLevel(request.getBatteryLevel());
        location.setRecordedAt(LocalDateTime.now());

        deliveryLocationRepository.save(location);
    }

    @Override
    public DeliveryLocationResponse getDeliveryLocation(Long customerId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException("Order not found", HttpStatus.NOT_FOUND));

        // Only the customer who placed the order can poll location
        if (!order.getCustomer().getId().equals(customerId))
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);

        // Only available when order is OUT_FOR_DELIVERY
        if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY)
            throw new ApiException("Delivery tracking is not active for this order", HttpStatus.BAD_REQUEST);

        DeliveryLocation location = deliveryLocationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ApiException("Delivery person location not available yet", HttpStatus.NOT_FOUND));

        return toResponse(location);
    }

    private DeliveryLocationResponse toResponse(DeliveryLocation location) {
        DeliveryLocationResponse r = new DeliveryLocationResponse();
        r.setOrderId(location.getOrder().getId());
        r.setLatitude(location.getLatitude());
        r.setLongitude(location.getLongitude());
        r.setAccuracy(location.getAccuracy());
        r.setBatteryLevel(location.getBatteryLevel());
        r.setRecordedAt(location.getRecordedAt());
//        r.setSecondsSinceUpdate(ChronoUnit.SECONDS.between(location.getRecordedAt(), LocalDateTime.now()));
        return r;
    }
}
