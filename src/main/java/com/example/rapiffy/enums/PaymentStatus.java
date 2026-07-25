package com.example.rapiffy.enums;

/**
 * Tracks the payment lifecycle for a ParentOrder.
 */
public enum PaymentStatus {
    PENDING,              // Razorpay order created, waiting for customer to pay
    PAID,                 // Payment captured successfully
    PARTIALLY_REFUNDED,   // Some sub-orders refunded, rest still active
    FULLY_REFUNDED,       // All sub-orders refunded — full amount returned
    FAILED                // Payment attempt failed
}
