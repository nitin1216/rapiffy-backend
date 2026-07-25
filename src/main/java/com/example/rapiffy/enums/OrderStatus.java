package com.example.rapiffy.enums;

public enum OrderStatus {
    PAYMENT_PENDING,   // Order created, waiting for customer to complete payment
    PENDING,           // Payment done, waiting for Admin to confirm
    CONFIRMED,         // Admin accepted the order
    READY,             // Admin packed and ready for pickup/delivery
    OUT_FOR_DELIVERY,  // Delivery person picked up
    DELIVERED,         // Order delivered to customer
    CANCELLED,         // Cancelled by customer before admin confirmed
    REJECTED           // Admin rejected the order (e.g. out of stock)
}
