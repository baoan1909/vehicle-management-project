package com.ban.vehicle_management.domain.iam.account.policy;

import com.ban.vehicle_management.application.iam.account.port.out.VerificationEmailRateLimitPortOut;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificationEmailResendPolicyTest {

    private final VerificationEmailResendPolicy policy = new VerificationEmailResendPolicy();

    @Test
    void shouldRejectWhenCooldownNotReached() {
        Instant now = Instant.parse("2026-05-30T00:01:00Z");
        VerificationEmailRateLimitPortOut.VerificationEmailRateLimitSnapshot snapshot =
                new VerificationEmailRateLimitPortOut.VerificationEmailRateLimitSnapshot(
                        Optional.of(now.minusSeconds(10)),
                        Optional.of(now.minusSeconds(3500)),
                        1
                );

        VerificationEmailResendPolicy.VerificationEmailResendDecision decision = policy.evaluate(now, snapshot);

        assertFalse(decision.allowed());
        assertTrue(decision.retryAfterSeconds() > 0);
    }

    @Test
    void shouldRejectWhenHourlyLimitReached() {
        Instant now = Instant.parse("2026-05-30T00:59:00Z");
        VerificationEmailRateLimitPortOut.VerificationEmailRateLimitSnapshot snapshot =
                new VerificationEmailRateLimitPortOut.VerificationEmailRateLimitSnapshot(
                        Optional.of(now.minusSeconds(120)),
                        Optional.of(now.minusSeconds(3500)),
                        5
                );

        VerificationEmailResendPolicy.VerificationEmailResendDecision decision = policy.evaluate(now, snapshot);

        assertFalse(decision.allowed());
        assertTrue(decision.retryAfterSeconds() > 0);
    }

    @Test
    void shouldAllowWhenUnderLimits() {
        Instant now = Instant.parse("2026-05-30T00:59:00Z");
        VerificationEmailRateLimitPortOut.VerificationEmailRateLimitSnapshot snapshot =
                new VerificationEmailRateLimitPortOut.VerificationEmailRateLimitSnapshot(
                        Optional.of(now.minusSeconds(120)),
                        Optional.of(now.minusSeconds(3500)),
                        4
                );

        VerificationEmailResendPolicy.VerificationEmailResendDecision decision = policy.evaluate(now, snapshot);

        assertTrue(decision.allowed());
    }
}
