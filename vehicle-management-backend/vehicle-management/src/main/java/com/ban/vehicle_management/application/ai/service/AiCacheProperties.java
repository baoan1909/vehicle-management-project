package com.ban.vehicle_management.application.ai.service;

import jakarta.validation.constraints.AssertTrue;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * TTL and safety policy for AI Redis caches. All TTLs are fixed (no sliding
 * expiration); a +-10% jitter is applied at write time to avoid stampedes.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.ai.cache")
public class AiCacheProperties {

    private boolean enabled = true;
    private Duration queryEmbeddingTtl = Duration.ofHours(24);
    private Duration publicRetrievalTtl = Duration.ofMinutes(15);
    private Duration customerRetrievalTtl = Duration.ofMinutes(10);
    private Duration tenantRetrievalTtl = Duration.ofMinutes(5);
    private Duration negativeRetrievalTtl = Duration.ofSeconds(30);
    private Duration publicGroundedAnswerTtl = Duration.ofMinutes(15);
    private Duration customerGroundedAnswerTtl = Duration.ofMinutes(5);
    private Duration tenantGroundedAnswerTtl = Duration.ofMinutes(5);
    private Duration singleFlightLockTtl = Duration.ofSeconds(45);
    private Duration singleFlightWait = Duration.ofSeconds(2);
    private int maxModelsPerJobAttempt = 3;
    private int ttlJitterPercent = 10;
    private String keyHmacSecret = "";
    private int maxCachedQueryChars = 500;
    private int maxCachedAnswerChars = 4000;
    private int maxCachedCitations = 10;
    private int maxCachedEvidence = 10;
    private int maxVectorDimensions = 4096;
    private long maxPayloadBytes = 256 * 1024L;

    @AssertTrue(message = "AI cache TTL/wait durations must be greater than zero")
    public boolean areDurationsValid() {
        return isPositive(queryEmbeddingTtl)
                && isPositive(publicRetrievalTtl)
                && isPositive(customerRetrievalTtl)
                && isPositive(tenantRetrievalTtl)
                && isPositive(negativeRetrievalTtl)
                && isPositive(publicGroundedAnswerTtl)
                && isPositive(customerGroundedAnswerTtl)
                && isPositive(tenantGroundedAnswerTtl)
                && isPositive(singleFlightLockTtl)
                && isPositive(singleFlightWait);
    }

    private boolean isPositive(Duration value) {
        return value != null && !value.isZero() && !value.isNegative();
    }
}
