package com.ban.vehicle_management.infrastructure.cache;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.application.ai.service.AiCacheProperties;
import com.ban.vehicle_management.application.ai.service.SensitiveQueryGuard;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiCacheKeyFactoryTest {

    private AiCacheKeyFactory keys;
    private SensitiveQueryGuard guard;

    @BeforeEach
    void setUp() {
        AiCacheProperties cacheProperties = new AiCacheProperties();
        cacheProperties.setKeyHmacSecret("test-secret-32bytes-minimum-length!!");
        keys = new AiCacheKeyFactory(new RedisProperties(), cacheProperties);
        guard = new SensitiveQueryGuard();
    }

    @Test
    void rawQueryNeverAppearsInKey() {
        String raw = "quy trinh dang ky the xe";
        String key = keys.embeddingKey("GEMINI", "gemini-embedding-001", 768, "rag-qa-v1", raw);
        assertFalse(key.contains("quy trinh"));
        assertTrue(key.startsWith("vm:local:vehicle-management:v1:embedding-query:"));
    }

    @Test
    void piiQueriesAreNotCacheable() {
        assertFalse(guard.isCacheable("lien he user@example.com"));
        assertFalse(guard.isCacheable("bien so 51H-12345 cua toi"));
        assertFalse(guard.isCacheable("ma phieu AB-1234"));
        assertFalse(guard.isCacheable("token: Bearer abc123"));
        assertTrue(guard.isCacheable("quy trinh dang ky the xe nhu the nao"));
    }

    @Test
    void tenantIsolationInFingerprint() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        assertFalse(keys.tenantFingerprint(tenantA).equals(keys.tenantFingerprint(tenantB)));
        assertFalse(keys.tenantFingerprint(null).equals(keys.tenantFingerprint(tenantA)));
    }

    @Test
    void scopeIsolationInFingerprint() {
        String publicFp = keys.scopeFingerprint(List.of("PUBLIC"));
        String customerFp = keys.scopeFingerprint(List.of("PUBLIC", "CUSTOMER"));
        assertFalse(publicFp.equals(customerFp));
    }

    @Test
    void retrievalKeyChangesWithIndexAndPolicy() {
        UUID indexA = UUID.randomUUID();
        UUID indexB = UUID.randomUUID();
        String keyA = keys.retrievalKey(indexA, "checksum-1", "retrieval-policy-v1",
                "retrieval-threshold-v1", List.of("PUBLIC"), null, 5, "abc");
        String keyB = keys.retrievalKey(indexB, "checksum-1", "retrieval-policy-v1",
                "retrieval-threshold-v1", List.of("PUBLIC"), null, 5, "abc");
        assertFalse(keyA.equals(keyB));
    }
}
