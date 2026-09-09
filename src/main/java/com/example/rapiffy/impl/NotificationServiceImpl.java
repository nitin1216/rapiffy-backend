package com.example.rapiffy.impl;

import com.example.rapiffy.dto.NotificationResponse;
import com.example.rapiffy.enums.OrderStatus;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.Notification;
import com.example.rapiffy.model.User;
import com.example.rapiffy.repos.NotificationRepository;
import com.example.rapiffy.repos.UserRepository;
import com.example.rapiffy.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    public void send(Long userId, String title, String message, Long orderId, String orderNumber, OrderStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        Notification n = new Notification();
        n.setUser(user);
        n.setTitle(title);
        n.setMessage(message);
        n.setOrderId(orderId);
        n.setOrderNumber(orderNumber);
        n.setOrderStatus(status);
        notificationRepository.save(n);
    }

    @Override
    public List<NotificationResponse> getMyNotifications(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        return notificationRepository.findByUserOrderByCreatedAtDesc(user)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public void markAsRead(Long userId, Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException("Notification not found", HttpStatus.NOT_FOUND));
        if (!n.getUser().getId().equals(userId))
            throw new ApiException("Not authorized", HttpStatus.FORBIDDEN);
        n.setRead(true);
        notificationRepository.save(n);
    }

    @Override
    public long getUnreadCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    private NotificationResponse toResponse(Notification n) {
        NotificationResponse r = new NotificationResponse();
        r.setId(n.getId());
        r.setTitle(n.getTitle());
        r.setMessage(n.getMessage());
        r.setOrderId(n.getOrderId());
        r.setOrderNumber(n.getOrderNumber());
        r.setOrderStatus(n.getOrderStatus());
        r.setRead(n.isRead());
        r.setCreatedAt(n.getCreatedAt());
        return r;
    }
}
