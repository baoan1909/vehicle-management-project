package com.ban.vehicle_management.infrastructure.security.ratelimit.adapter;

import com.ban.vehicle_management.application.iam.account.port.out.VerificationEmailRateLimitPortOut;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryVerificationEmailRateLimitSecurityAdapterTest {

    private final InMemoryVerificationEmailRateLimitSecurityAdapter adapter = new InMemoryVerificationEmailRateLimitSecurityAdapter();

    @Test
    void shouldReturnSnapshotForAttemptsInsideWindowOnly() {
        String email = "user@example.com";
        Instant firstAttempt = Instant.parse("2026-05-30T00:00:00Z");
        Instant secondAttempt = Instant.parse("2026-05-30T00:10:00Z");
        Instant windowStart = Instant.parse("2026-05-30T00:05:00Z");

        adapter.saveAttempt(email, firstAttempt);
        adapter.saveAttempt(email, secondAttempt);

        VerificationEmailRateLimitPortOut.VerificationEmailRateLimitSnapshot snapshot =
                adapter.loadSnapshot(email, windowStart);

        assertEquals(1, snapshot.attemptsInWindow());
        assertEquals(secondAttempt, snapshot.latestAttemptAt().orElseThrow());
        assertEquals(secondAttempt, snapshot.earliestAttemptAtInWindow().orElseThrow());
    }

    @Test
    void shouldKeepChronologicalOrderForLatestAndEarliest() {
        String email = "another-user@example.com";
        Instant firstAttempt = Instant.parse("2026-05-30T00:00:00Z");
        Instant secondAttempt = Instant.parse("2026-05-30T00:20:00Z");
        Instant thirdAttempt = Instant.parse("2026-05-30T00:40:00Z");

        adapter.saveAttempt(email, firstAttempt);
        adapter.saveAttempt(email, secondAttempt);
        adapter.saveAttempt(email, thirdAttempt);

        VerificationEmailRateLimitPortOut.VerificationEmailRateLimitSnapshot snapshot =
                adapter.loadSnapshot(email, Instant.parse("2026-05-30T00:00:00Z"));

        assertEquals(3, snapshot.attemptsInWindow());
        assertEquals(firstAttempt, snapshot.earliestAttemptAtInWindow().orElseThrow());
        assertEquals(thirdAttempt, snapshot.latestAttemptAt().orElseThrow());
        assertTrue(snapshot.latestAttemptAt().isPresent());
    }
}
