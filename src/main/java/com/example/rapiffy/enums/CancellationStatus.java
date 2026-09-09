package com.example.rapiffy.enums;

public enum CancellationStatus {
    REQUESTED,    // Customer submitted the cancellation request
    APPROVED,     // Admin approved — refund will be triggered
    REJECTED      // Admin rejected with a reason
}
