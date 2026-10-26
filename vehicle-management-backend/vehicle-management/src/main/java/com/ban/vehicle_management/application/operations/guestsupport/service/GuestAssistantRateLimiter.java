package com.ban.vehicle_management.application.operations.guestsupport.service;

import com.ban.vehicle_management.application.operations.guestsupport.config.GuestSupportProperties;
import com.ban.vehicle_management.shared.exception.TooManyRequestsException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class GuestAssistantRateLimiter {
    private final GuestSupportProperties properties;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong requestCount = new AtomicLong();

    public GuestAssistantRateLimiter(GuestSupportProperties properties) {
        this.properties = properties;
    }

    public void requireAllowed(String key) {
        Instant now = Instant.now();
        if ((requestCount.incrementAndGet() & 255L) == 0L) {
            windows.entrySet().removeIf(entry -> isExpired(entry.getValue(), now));
        }
        if (!windows.containsKey(key) && windows.size() >= properties.getMaxAssistantRateKeys()) {
            throw rateLimitExceeded();
        }
        Window current = windows.compute(key, (ignored, previous) -> {
            if (previous == null || isExpired(previous, now)) {
                return new Window(now, 1);
            }
            return new Window(previous.startedAt(), previous.count() + 1);
        });
        if (current.count() > properties.getMaxAssistantMessagesPerWindow()) {
            throw rateLimitExceeded();
        }
    }

    private boolean isExpired(Window window, Instant now) {
        return !window.startedAt().plus(properties.getRateWindow()).isAfter(now);
    }

    private TooManyRequestsException rateLimitExceeded() {
        return new TooManyRequestsException("Bạn đã gửi quá nhiều câu hỏi. Vui lòng thử lại sau");
    }

    private record Window(Instant startedAt, int count) {
    }
}
