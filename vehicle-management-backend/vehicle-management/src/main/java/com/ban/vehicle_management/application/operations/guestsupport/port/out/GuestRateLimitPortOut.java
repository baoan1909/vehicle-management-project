package com.ban.vehicle_management.application.operations.guestsupport.port.out;

import com.ban.vehicle_management.application.operations.guestsupport.model.GuestRateLimitBucket;
import com.ban.vehicle_management.application.operations.guestsupport.model.GuestRateLimitDecision;
import java.time.Duration;

public interface GuestRateLimitPortOut {

    GuestRateLimitDecision consume(
            GuestRateLimitBucket bucket,
            String hashedIdentity,
            int limit,
            Duration window
    );
}
