package com.example.rapiffy.enums;

/**
 * Who initiated the cancellation.
 */
public enum CancelledBy {
    CUSTOMER,     // Customer cancelled before admin confirmed
    ADMIN,        // Shopkeeper rejected the order
    SYSTEM        // Auto-cancelled (e.g. payment timeout, fraud detection)
}
