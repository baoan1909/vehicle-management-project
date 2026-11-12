package com.ban.vehicle_management.shared.enumeration.billing;

public enum BillingOutboxStatus {
    PENDING,
    PROCESSING,
    RETRY,
    PROCESSED,
    DEAD_LETTER
}
