package com.example.rapiffy.enums;

public enum ReturnStatus {
    REQUESTED,              // Customer submitted the return request
    ADMIN_REVIEWING,        // Admin has opened and is reviewing the request
    APPROVED,               // Admin approved — refund will be triggered
    REJECTED,               // Admin rejected with a reason
    REFUNDED,               // Online payment refunded via Razorpay to original source
    CASH_REFUND_ON_RETURN   // COD order — customer will receive cash from delivery person at return pickup
}
