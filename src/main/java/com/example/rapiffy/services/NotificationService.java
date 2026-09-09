package com.example.rapiffy.services;

import com.example.rapiffy.dto.NotificationResponse;
import com.example.rapiffy.enums.OrderStatus;

import java.util.List;

public interface NotificationService {

    void send(Long userId, String title, String message, Long orderId, String orderNumber, OrderStatus status);

    List<NotificationResponse> getMyNotifications(Long userId);

    void markAsRead(Long userId, Long notificationId);

    long getUnreadCount(Long userId);
}
