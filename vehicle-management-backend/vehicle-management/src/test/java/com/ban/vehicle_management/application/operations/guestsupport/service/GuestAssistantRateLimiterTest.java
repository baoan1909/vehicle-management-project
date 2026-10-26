package com.ban.vehicle_management.application.operations.guestsupport.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.application.operations.guestsupport.config.GuestSupportProperties;
import com.ban.vehicle_management.shared.exception.TooManyRequestsException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class GuestAssistantRateLimiterTest {

    @Test
    void rejectsMessagesBeyondThePerKeyWindow() {
        GuestSupportProperties properties = new GuestSupportProperties();
        properties.setMaxAssistantMessagesPerWindow(1);
        GuestAssistantRateLimiter limiter = new GuestAssistantRateLimiter(properties);

        assertDoesNotThrow(() -> limiter.requireAllowed("ip:one"));
        assertThrows(TooManyRequestsException.class, () -> limiter.requireAllowed("ip:one"));
    }

    @Test
    void boundsDistinctRateKeysToProtectMemory() {
        GuestSupportProperties properties = new GuestSupportProperties();
        properties.setMaxAssistantRateKeys(1);
        GuestAssistantRateLimiter limiter = new GuestAssistantRateLimiter(properties);

        assertDoesNotThrow(() -> limiter.requireAllowed("ip:one"));
        assertThrows(TooManyRequestsException.class, () -> limiter.requireAllowed("ip:two"));
    }

    @Test
    void expiredWindowCanBeReused() {
        GuestSupportProperties properties = new GuestSupportProperties();
        properties.setMaxAssistantMessagesPerWindow(1);
        properties.setRateWindow(Duration.ZERO);
        GuestAssistantRateLimiter limiter = new GuestAssistantRateLimiter(properties);

        limiter.requireAllowed("ip:one");

        assertDoesNotThrow(() -> limiter.requireAllowed("ip:one"));
    }
}
