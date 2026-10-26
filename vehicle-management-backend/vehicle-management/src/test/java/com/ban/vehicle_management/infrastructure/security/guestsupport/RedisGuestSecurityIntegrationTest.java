package com.ban.vehicle_management.infrastructure.security.guestsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.operations.guestsupport.config.GuestSupportProperties;
import com.ban.vehicle_management.application.operations.guestsupport.model.GuestRateLimitBucket;
import com.ban.vehicle_management.infrastructure.cache.RedisProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class RedisGuestSecurityIntegrationTest {

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7.4.1-alpine"))
            .withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;
    private RedisProperties redisProperties;
    private GuestSupportProperties guestProperties;

    @BeforeAll
    static void connect() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
    }

    @AfterAll
    static void disconnect() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        redisProperties = new RedisProperties();
        redisProperties.setEnabled(true);
        redisProperties.setEnvironment("integration");
        redisProperties.setService("vehicle-management");
        guestProperties = new GuestSupportProperties();
        guestProperties.setRateLimitLocalFallbackEnabled(false);
    }

    @Test
    void rateLimitIsSharedAtomicExpiringAndUsesOnlyOpaqueIdentityKeys() throws Exception {
        RedisGuestRateLimitAdapter first = rateLimitAdapter();
        RedisGuestRateLimitAdapter second = rateLimitAdapter();
        String rawIdentity = "203.0.113.5";
        String identityDigest = sha256(rawIdentity);
        int attempts = 40;
        int limit = 20;
        Duration window = Duration.ofSeconds(30);

        try (var executor = Executors.newFixedThreadPool(8)) {
            List<Callable<Boolean>> calls = java.util.stream.IntStream.range(0, attempts)
                    .mapToObj(index -> (Callable<Boolean>) () -> (index % 2 == 0 ? first : second)
                            .consume(GuestRateLimitBucket.ASSISTANT_IP, identityDigest, limit, window)
                            .allowed())
                    .toList();
            long allowed = executor.invokeAll(calls).stream()
                    .filter(future -> {
                        try {
                            return future.get();
                        } catch (Exception exception) {
                            throw new AssertionError(exception);
                        }
                    })
                    .count();
            assertEquals(limit, allowed);
        }

        Set<String> keys = redisTemplate.keys("vm:integration:vehicle-management:v1:public-support-rate-limit:*");
        assertEquals(1, keys.size());
        String key = keys.iterator().next();
        assertFalse(key.contains(rawIdentity));
        assertTrue(key.endsWith(identityDigest));
        Long ttlMillis = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
        assertTrue(ttlMillis != null && ttlMillis > 0 && ttlMillis <= window.toMillis());
    }

    @Test
    void captchaTokenClaimAndProofConsumptionAreSharedAtomicAndExpiring() throws Exception {
        RedisCaptchaProofStoreAdapter first = proofStoreAdapter();
        RedisCaptchaProofStoreAdapter second = proofStoreAdapter();
        String tokenDigest = sha256("provider-token");
        String proofDigest = sha256("opaque-proof");
        String bindingDigest = sha256("ASSISTANT|session|ip");
        Duration ttl = Duration.ofSeconds(30);

        assertTrue(first.claimChallengeToken(tokenDigest, ttl));
        assertFalse(second.claimChallengeToken(tokenDigest, ttl));
        first.storeProof(proofDigest, bindingDigest, ttl);

        try (var executor = Executors.newFixedThreadPool(8)) {
            List<Callable<Boolean>> calls = java.util.stream.IntStream.range(0, 24)
                    .mapToObj(index -> (Callable<Boolean>) () -> (index % 2 == 0 ? first : second)
                            .consumeProof(proofDigest, bindingDigest))
                    .toList();
            long consumed = executor.invokeAll(calls).stream()
                    .filter(future -> {
                        try {
                            return future.get();
                        } catch (Exception exception) {
                            throw new AssertionError(exception);
                        }
                    })
                    .count();
            assertEquals(1, consumed);
        }

        Set<String> tokenKeys = redisTemplate.keys("vm:integration:vehicle-management:v1:public-support:captcha-token:*");
        assertEquals(1, tokenKeys.size());
        assertTrue(redisTemplate.getExpire(tokenKeys.iterator().next(), TimeUnit.MILLISECONDS) > 0);
        assertTrue(redisTemplate.keys("vm:integration:vehicle-management:v1:public-support:captcha-proof:*").isEmpty());
    }

    private RedisGuestRateLimitAdapter rateLimitAdapter() {
        return new RedisGuestRateLimitAdapter(
                redisProperties,
                guestProperties,
                provider(),
                new GuestSupportSecurityMetrics(new SimpleMeterRegistry()));
    }

    private RedisCaptchaProofStoreAdapter proofStoreAdapter() {
        return new RedisCaptchaProofStoreAdapter(redisProperties, guestProperties, provider());
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<StringRedisTemplate> provider() {
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(redisTemplate);
        return provider;
    }

    private String sha256(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
        return java.util.HexFormat.of().formatHex(digest);
    }
}
