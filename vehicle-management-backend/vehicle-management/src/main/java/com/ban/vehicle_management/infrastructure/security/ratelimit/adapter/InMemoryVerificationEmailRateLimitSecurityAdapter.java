package com.ban.vehicle_management.infrastructure.security.ratelimit.adapter;

import com.ban.vehicle_management.application.iam.account.port.out.VerificationEmailRateLimitPortOut;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryVerificationEmailRateLimitSecurityAdapter implements VerificationEmailRateLimitPortOut {

    private final Map<String, Deque<Instant>> attemptsByEmail = new ConcurrentHashMap<>();

    @Override
    public VerificationEmailRateLimitSnapshot loadSnapshot(String normalizedEmail, Instant windowStartAt) {
        Deque<Instant> attempts = attemptsByEmail.computeIfAbsent(normalizedEmail, ignored -> new ArrayDeque<>());
        synchronized (attempts) {
            pruneAttemptsBeforeWindow(attempts, windowStartAt);

            return new VerificationEmailRateLimitSnapshot(
                    Optional.ofNullable(attempts.peekLast()),
                    Optional.ofNullable(attempts.peekFirst()),
                    attempts.size()
            );
        }
    }

    @Override
    public void saveAttempt(String normalizedEmail, Instant attemptedAt) {
        Deque<Instant> attempts = attemptsByEmail.computeIfAbsent(normalizedEmail, ignored -> new ArrayDeque<>());
        synchronized (attempts) {
            attempts.addLast(attemptedAt);
        }
    }

    private void pruneAttemptsBeforeWindow(Deque<Instant> attempts, Instant windowStartAt) {
        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(windowStartAt)) {
            attempts.removeFirst();
        }
    }
}
