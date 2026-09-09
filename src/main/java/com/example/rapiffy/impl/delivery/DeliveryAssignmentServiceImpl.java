package com.example.rapiffy.impl.delivery;

import com.example.rapiffy.dto.delivery.AssignDeliveryRequest;
import com.example.rapiffy.dto.delivery.AssignDeliveryResponse;
import com.example.rapiffy.enums.OrderStatus;
import com.example.rapiffy.enums.Roles;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.DeliveryPerson;
import com.example.rapiffy.model.Order;
import com.example.rapiffy.model.Profile;
import com.example.rapiffy.model.User;
import com.example.rapiffy.repos.DeliveryPersonRepository;
import com.example.rapiffy.repos.OrderRepository;
import com.example.rapiffy.repos.ProfileRepository;
import com.example.rapiffy.repos.UserRepository;
import com.example.rapiffy.services.delivery.DeliveryAssignmentService;
import com.example.rapiffy.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DeliveryAssignmentServiceImpl implements DeliveryAssignmentService {

    private final OrderRepository orderRepository;
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final DeliveryPersonRepository deliveryPersonRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public AssignDeliveryResponse assignDeliveryPerson(Long adminUserId, Long orderId, AssignDeliveryRequest request) {
        Profile shop = profileRepository.findByUserId(adminUserId)
                .orElseThrow(() -> new ApiException("Shop profile not found", HttpStatus.NOT_FOUND));

        Order order = orderRepository.findByIdAndShop(orderId, shop)
                .orElseThrow(() -> new ApiException("Order not found", HttpStatus.NOT_FOUND));

        if (order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.READY && order.getStatus() != OrderStatus.DELIVERY_REJECTED)
            throw new ApiException("Order must be CONFIRMED, READY or DELIVERY_REJECTED to assign a delivery person", HttpStatus.BAD_REQUEST);

        User deliveryPerson = userRepository.findById(request.getDeliveryPersonUserId())
                .orElseThrow(() -> new ApiException("Delivery person not found", HttpStatus.NOT_FOUND));

        if (deliveryPerson.getRole() != Roles.DELIVERY)
            throw new ApiException("User is not a delivery person", HttpStatus.BAD_REQUEST);

        DeliveryPerson dp = deliveryPersonRepository.findByUserId(deliveryPerson.getId())
                .orElseThrow(() -> new ApiException("Delivery person not linked to any shop", HttpStatus.BAD_REQUEST));

        if (!dp.getShop().getId().equals(shop.getId()))
            throw new ApiException("Delivery person does not belong to this shop", HttpStatus.FORBIDDEN);

        order.setDeliveryPerson(deliveryPerson);
        order.setAssignedAt(LocalDateTime.now());
        orderRepository.save(order);

        notificationService.send(
                deliveryPerson.getId(),
                "New Order Assigned",
                "You have been assigned order " + order.getOrderNumber() + ". Please accept or reject.",
                order.getId(), order.getOrderNumber(), order.getStatus()
        );

        AssignDeliveryResponse response = new AssignDeliveryResponse();
        response.setOrderId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setStatus(order.getStatus());
        response.setDeliveryPersonUserId(deliveryPerson.getId());
        response.setDeliveryPersonPhone(deliveryPerson.getPhoneNumber());
        if (deliveryPerson.getFullName() != null)
            response.setDeliveryPersonName(
                deliveryPerson.getFullName().getFirstName() + " " + deliveryPerson.getFullName().getLastName()
            );
        response.setAssignedAt(order.getAssignedAt());
        return response;
    }
}
