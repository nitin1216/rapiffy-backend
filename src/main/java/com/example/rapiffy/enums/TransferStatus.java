package com.example.rapiffy.enums;

/**
 * Tracks the transfer lifecycle for shop settlements via Razorpay Route.
 */
public enum TransferStatus {
    PENDING,      // Transfer not yet created (payment not captured yet)
    CREATED,      // Transfer created in Razorpay
    SETTLED,      // Money settled to shop's bank account
    REVERSED,     // Transfer reversed (due to cancellation/refund)
    FAILED        // Transfer failed
}
