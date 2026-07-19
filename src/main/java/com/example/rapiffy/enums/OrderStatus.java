package com.example.rapiffy.enums;

public enum OrderStatus {
    PENDING,           // Customer placed order, waiting for Admin to confirm
    CONFIRMED,         // Admin accepted the order
    READY,             // Admin packed and ready for pickup/delivery
    OUT_FOR_DELIVERY,  // Delivery person picked up
    DELIVERED          // Order delivered to customer
}
