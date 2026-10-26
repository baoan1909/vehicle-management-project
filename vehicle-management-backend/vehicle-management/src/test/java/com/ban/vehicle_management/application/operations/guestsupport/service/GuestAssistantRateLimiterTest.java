package com.ban.vehicle_management.application.operations.guestsupport.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.operations.guestsupport.config.GuestSupportProperties;
import com.ban.vehicle_management.application.operations.guestsupport.model.GuestRateLimitBucket;
import com.ban.vehicle_management.application.operations.guestsupport.model.GuestRateLimitDecision;
import com.ban.vehicle_management.application.operations.guestsupport.port.out.GuestRateLimitPortOut;
import com.ban.vehicle_management.shared.exception.PublicSupportRateLimitException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GuestAssistantRateLimiterTest {

    @Mock GuestRateLimitPortOut port;

    @Test
    void delegatesToDistributedPortAndAllowsDecision() {
        GuestSupportProperties properties = new GuestSupportProperties();
        String digest = "a".repeat(64);
        when(port.consume(GuestRateLimitBucket.ASSISTANT_IP, digest,
                properties.getMaxAssistantMessagesPerWindow(), properties.getRateWindow()))
                .thenReturn(new GuestRateLimitDecision(true, 60));
        GuestAssistantRateLimiter limiter = new GuestAssistantRateLimiter(properties, port);

        assertDoesNotThrow(() -> limiter.requireAllowed(GuestRateLimitBucket.ASSISTANT_IP, digest));
        verify(port).consume(GuestRateLimitBucket.ASSISTANT_IP, digest,
                properties.getMaxAssistantMessagesPerWindow(), properties.getRateWindow());
    }

    @Test
    void rejectedDecisionPreservesRetryAfter() {
        GuestSupportProperties properties = new GuestSupportProperties();
        String digest = "b".repeat(64);
        when(port.consume(GuestRateLimitBucket.ASSISTANT_SESSION, digest,
                properties.getMaxAssistantMessagesPerWindow(), properties.getRateWindow()))
                .thenReturn(new GuestRateLimitDecision(false, 37));
        GuestAssistantRateLimiter limiter = new GuestAssistantRateLimiter(properties, port);

        PublicSupportRateLimitException exception = assertThrows(PublicSupportRateLimitException.class,
                () -> limiter.requireAllowed(GuestRateLimitBucket.ASSISTANT_SESSION, digest));
        assertEquals(37, exception.retryAfterSeconds());
    }
}
