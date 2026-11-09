package com.ban.vehicle_management.shared.enumeration.iam;

public enum AccountDeletionRequestStatus {
    GRACE_PERIOD,
    CANCELLED,
    PROCESSING,
    RETRY,
    REVIEW_REQUIRED,
    COMPLETED
}
