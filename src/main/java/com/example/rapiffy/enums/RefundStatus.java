package com.example.rapiffy.enums;

/**
 * Tracks the refund lifecycle.
 */
public enum RefundStatus {
    INITIATED,    // Refund request created in our system
    PROCESSING,   // Sent to Razorpay, awaiting completion
    COMPLETED,    // Razorpay confirmed refund is done
    FAILED        // Refund failed at gateway
}
