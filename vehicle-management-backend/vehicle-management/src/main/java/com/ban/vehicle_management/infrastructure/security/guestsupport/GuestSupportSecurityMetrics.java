package com.ban.vehicle_management.infrastructure.security.guestsupport;

import com.ban.vehicle_management.application.operations.guestsupport.model.GuestRateLimitBucket;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class GuestSupportSecurityMetrics {

    private final MeterRegistry registry;

    public GuestSupportSecurityMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void rateLimit(GuestRateLimitBucket bucket, String outcome) {
        registry.counter("public.support.rate.limit", "bucket", bucket.keySegment(), "outcome", outcome).increment();
    }

    public void captchaRejected(String reason) {
        registry.counter("public.support.captcha.rejected", "reason", reason).increment();
    }
}
