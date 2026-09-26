package com.ban.vehicle_management.domain.iam.account.policy;

import com.ban.vehicle_management.application.iam.account.port.out.VerificationEmailRateLimitPortOut;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class VerificationEmailResendPolicy {

    private final Duration cooldown;
    private final Duration rateWindow;
    private final int maxRequests;

    public VerificationEmailResendPolicy() {
        this(Duration.ofMinutes(1), Duration.ofHours(1), 5);
    }

    public VerificationEmailResendPolicy(Duration cooldown, Duration rateWindow, int maxRequests) {
        if (!isPositive(cooldown) || !isPositive(rateWindow) || maxRequests <= 0) {
            throw new IllegalArgumentException("Verification email time policy is invalid");
        }
        this.cooldown = cooldown;
        this.rateWindow = rateWindow;
        this.maxRequests = maxRequests;
    }

    public VerificationEmailResendDecision evaluate(
            Instant requestedAt,
            VerificationEmailRateLimitPortOut.VerificationEmailRateLimitSnapshot snapshot
    ) {
        Optional<Instant> latestAttemptAt = snapshot.latestAttemptAt();
        if (latestAttemptAt.isPresent()) {
            long elapsedSeconds = Duration.between(latestAttemptAt.get(), requestedAt).getSeconds();
            if (elapsedSeconds < cooldown.getSeconds()) {
                return new VerificationEmailResendDecision(false, cooldown.getSeconds() - elapsedSeconds);
            }
        }

        if (snapshot.attemptsInWindow() >= maxRequests) {
            long retryAfterSeconds = snapshot.earliestAttemptAtInWindow()
                    .map(earliestAttemptAt -> Duration.between(
                            requestedAt,
                            earliestAttemptAt.plus(rateWindow)
                    ).getSeconds())
                    .orElse(cooldown.getSeconds());
            if (retryAfterSeconds <= 0) {
                retryAfterSeconds = 1;
            }
            return new VerificationEmailResendDecision(false, retryAfterSeconds);
        }

        return new VerificationEmailResendDecision(true, 0);
    }

    public Instant windowStartAt(Instant requestedAt) {
        return requestedAt.minus(rateWindow);
    }

    private boolean isPositive(Duration value) {
        return value != null && !value.isZero() && !value.isNegative();
    }

    public record VerificationEmailResendDecision(
            boolean allowed,
            long retryAfterSeconds
    ) {
    }
}
