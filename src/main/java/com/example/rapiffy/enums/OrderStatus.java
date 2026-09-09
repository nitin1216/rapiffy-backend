package com.example.rapiffy.enums;

public enum OrderStatus {
    PAYMENT_PENDING,   // Order created, waiting for customer to complete payment
    PENDING,           // Payment done, waiting for Admin to confirm
    CONFIRMED,         // Admin accepted the order
    DELIVERY_ACCEPTED, // Delivery boy accepted the assigned order
    DELIVERY_REJECTED, // Delivery boy rejected the assigned order (Admin must re-assign)
    READY,             // Admin packed and ready for pickup/delivery
    COMING_TO_PICK,    // Delivery boy is on the way to shop for pickup
    OUT_FOR_DELIVERY,  // Delivery person picked up, heading to customer
    DELIVERED,         // Order delivered to customer
    CANCELLED,         // Cancelled by customer before admin confirmed
    REJECTED           // Admin rejected the order (e.g. out of stock)
}
