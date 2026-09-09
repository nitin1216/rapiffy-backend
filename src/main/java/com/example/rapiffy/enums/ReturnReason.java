package com.example.rapiffy.enums;

public enum ReturnReason {
    MISSING,    // Item was not in the delivery
    EXPIRED,    // Item was past its expiry date
    OVERSIZED,  // Item size/weight did not match what was ordered
    DAMAGED     // Item was physically damaged on arrival
}
