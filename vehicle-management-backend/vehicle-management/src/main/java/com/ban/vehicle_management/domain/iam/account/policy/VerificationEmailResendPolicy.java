package com.ban.vehicle_management.domain.iam.account.policy;

import com.ban.vehicle_management.application.iam.account.port.out.VerificationEmailRateLimitPortOut;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
public class VerificationEmailResendPolicy {

    private static final Duration COOLDOWN = Duration.ofSeconds(60);
    private static final Duration HOURLY_WINDOW = Duration.ofHours(1);
    private static final int MAX_REQUESTS_PER_HOUR = 5;

    public VerificationEmailResendDecision evaluate(
            Instant requestedAt,
            VerificationEmailRateLimitPortOut.VerificationEmailRateLimitSnapshot snapshot
    ) {
        Optional<Instant> latestAttemptAt = snapshot.latestAttemptAt();
        if (latestAttemptAt.isPresent()) {
            long elapsedSeconds = Duration.between(latestAttemptAt.get(), requestedAt).getSeconds();
            if (elapsedSeconds < COOLDOWN.getSeconds()) {
                return new VerificationEmailResendDecision(false, COOLDOWN.getSeconds() - elapsedSeconds);
            }
        }

        if (snapshot.attemptsInWindow() >= MAX_REQUESTS_PER_HOUR) {
            long retryAfterSeconds = snapshot.earliestAttemptAtInWindow()
                    .map(earliestAttemptAt -> Duration.between(
                            requestedAt,
                            earliestAttemptAt.plus(HOURLY_WINDOW)
                    ).getSeconds())
                    .orElse(COOLDOWN.getSeconds());
            if (retryAfterSeconds <= 0) {
                retryAfterSeconds = 1;
            }
            return new VerificationEmailResendDecision(false, retryAfterSeconds);
        }

        return new VerificationEmailResendDecision(true, 0);
    }

    public Instant windowStartAt(Instant requestedAt) {
        return requestedAt.minus(HOURLY_WINDOW);
    }

    public record VerificationEmailResendDecision(
            boolean allowed,
            long retryAfterSeconds
    ) {
    }
}
