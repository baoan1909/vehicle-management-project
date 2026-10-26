package com.ban.vehicle_management.application.operations.guestsupport.service;

import com.ban.vehicle_management.application.operations.guestsupport.config.GuestSupportProperties;
import com.ban.vehicle_management.application.operations.guestsupport.model.GuestRateLimitBucket;
import com.ban.vehicle_management.application.operations.guestsupport.model.GuestRateLimitDecision;
import com.ban.vehicle_management.application.operations.guestsupport.port.out.GuestRateLimitPortOut;
import com.ban.vehicle_management.shared.exception.PublicSupportRateLimitException;
import org.springframework.stereotype.Component;

@Component
public class GuestAssistantRateLimiter {
    private final GuestSupportProperties properties;
    private final GuestRateLimitPortOut rateLimitPortOut;

    public GuestAssistantRateLimiter(GuestSupportProperties properties, GuestRateLimitPortOut rateLimitPortOut) {
        this.properties = properties;
        this.rateLimitPortOut = rateLimitPortOut;
    }

    public void requireAllowed(GuestRateLimitBucket bucket, String hashedIdentity) {
        GuestRateLimitDecision decision = rateLimitPortOut.consume(
                bucket,
                hashedIdentity,
                properties.getMaxAssistantMessagesPerWindow(),
                properties.getRateWindow());
        if (!decision.allowed()) {
            throw new PublicSupportRateLimitException(
                    "PUBLIC_SUPPORT_RATE_LIMITED",
                    "Bạn đã gửi quá nhiều câu hỏi. Vui lòng thử lại sau",
                    decision.retryAfterSeconds());
        }
    }
}
