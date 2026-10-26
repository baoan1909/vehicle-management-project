package com.ban.vehicle_management.application.operations.guestsupport.model;

import java.time.Instant;

public record CaptchaVerificationResult(
        boolean success,
        String action,
        String hostname,
        Instant challengedAt,
        String rejectionReason
) {
}
