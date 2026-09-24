package com.ban.vehicle_management.infrastructure.cache;

import com.ban.vehicle_management.application.ai.service.AiCacheProperties;
import com.ban.vehicle_management.domain.ai.model.VietnameseQueryNormalizer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Server-generated Redis key factory. Raw user queries never appear in keys;
 * the normalized query is HMAC-SHA-256 hashed with a configured secret.
 */
@Component
public class AiCacheKeyFactory {

    public static final String NORMALIZATION_VERSION = "query-norm-v1";
    public static final int SCHEMA_VERSION = 1;

    private final RedisProperties redisProperties;
    private final AiCacheProperties cacheProperties;

    public AiCacheKeyFactory(RedisProperties redisProperties, AiCacheProperties cacheProperties) {
        this.redisProperties = redisProperties;
        this.cacheProperties = cacheProperties;
    }

    public boolean hmacReady() {
        return cacheProperties.getKeyHmacSecret() != null && !cacheProperties.getKeyHmacSecret().isBlank();
    }

    private String prefix(String namespace) {
        return "vm:" + redisProperties.getEnvironment() + ":" + redisProperties.getService() + ":v1:" + namespace + ":";
    }

    public String embeddingKey(String provider, String modelId, int dimension,
            String embeddingPromptVersion, String normalizedQuery) {
        return prefix("embedding-query") + sanitize(provider) + ":" + sanitize(modelId) + ":" + dimension + ":"
                + sanitize(embeddingPromptVersion) + ":" + NORMALIZATION_VERSION + ":" + queryHmac(normalizedQuery);
    }

    public String retrievalKey(UUID activeIndexVersionId, String contentChecksum,
            String retrievalPolicyVersion, String thresholdVersion,
            List<String> scopes, UUID tenantId, int topK, String normalizedQuery) {
        return prefix("knowledge-retrieval") + activeIndexVersionId + ":" + sanitize(contentChecksum) + ":"
                + sanitize(retrievalPolicyVersion) + ":" + sanitize(thresholdVersion) + ":"
                + scopeFingerprint(scopes) + ":" + tenantFingerprint(tenantId) + ":" + topK + ":"
                + queryHmac(normalizedQuery);
    }

    public String groundedAnswerKey(UUID activeIndexVersionId, String contentChecksum,
            String promptVersion, String generationPolicyFingerprint,
            List<String> scopes, UUID tenantId, String language, String normalizedQuery) {
        return prefix("grounded-answer") + activeIndexVersionId + ":" + sanitize(contentChecksum) + ":"
                + sanitize(promptVersion) + ":" + sanitize(generationPolicyFingerprint) + ":"
                + scopeFingerprint(scopes) + ":" + tenantFingerprint(tenantId) + ":" + sanitize(language) + ":"
                + queryHmac(normalizedQuery);
    }

    public String circuitKey(String provider, String useCase, UUID configurationId) {
        return prefix("ai-circuit-breaker") + sanitize(provider) + ":" + sanitize(useCase) + ":" + configurationId;
    }

    public String singleFlightKey(String groundedAnswerKey) {
        return prefix("distributed-lock") + "grounded-answer:" + sha256Hex(groundedAnswerKey);
    }

    public String scopeFingerprint(List<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return sha256Hex("scope:PUBLIC");
        }
        List<String> sorted = scopes.stream().map(value -> value.toUpperCase(Locale.ROOT)).sorted().toList();
        return sha256Hex("scope:" + String.join(",", sorted)).substring(0, 16);
    }

    public String tenantFingerprint(UUID tenantId) {
        if (tenantId == null) {
            return "tenant-global";
        }
        return sha256Hex("tenant:" + tenantId).substring(0, 16);
    }

    public String queryHmac(String normalizedQuery) {
        String secret = cacheProperties.getKeyHmacSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("AI_CACHE_KEY_HMAC_SECRET is not configured");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(normalizedQuery.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 32);
        } catch (Exception exception) {
            throw new IllegalStateException("HMAC unavailable", exception);
        }
    }

    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((input == null ? "" : input).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "na";
        }
        String cleaned = value.strip().replaceAll("[^A-Za-z0-9._-]", "-");
        return cleaned.length() > 80 ? cleaned.substring(0, 80) : cleaned;
    }

    public static String normalizedForKey(String redactedQuery) {
        return VietnameseQueryNormalizer.normalize(redactedQuery).normalized();
    }
}
