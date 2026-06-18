package com.sheemab.shardedsagawallet.enums;

public enum IdempotencyStatus {
    PROCESSING,  // saga is running right now
    COMPLETED,   // saga finished, result stored
    FAILED       // saga failed — client can retry with same key
}
