package com.ban.vehicle_management.application.iam.account.port.out;

import java.time.Instant;
import java.util.Optional;

public interface VerificationEmailRateLimitPortOut {

    VerificationEmailRateLimitSnapshot loadSnapshot(String normalizedEmail, Instant windowStartAt);

    void saveAttempt(String normalizedEmail, Instant attemptedAt);

    record VerificationEmailRateLimitSnapshot(
            Optional<Instant> latestAttemptAt,
            Optional<Instant> earliestAttemptAtInWindow,
            int attemptsInWindow
    ) {
    }
}
