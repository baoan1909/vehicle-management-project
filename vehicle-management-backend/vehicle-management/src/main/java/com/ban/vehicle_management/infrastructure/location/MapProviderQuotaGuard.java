package com.ban.vehicle_management.infrastructure.location;

import com.ban.vehicle_management.infrastructure.cache.RedisProperties;
import com.ban.vehicle_management.shared.exception.MapRequestLimitException;
import com.ban.vehicle_management.shared.exception.ServiceUnavailableException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class MapProviderQuotaGuard {

    static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String QUOTA_EXHAUSTED_CODE = "MAP_DAILY_QUOTA_EXHAUSTED";
    private static final DefaultRedisScript<Long> ACQUIRE_QUOTA_SCRIPT = new DefaultRedisScript<>("""
            local current = tonumber(redis.call('GET', KEYS[1]) or '0')
            local requestLimit = tonumber(ARGV[1])
            if current >= requestLimit then
                return -1
            end
            current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], tonumber(ARGV[2]))
            end
            return current
            """, Long.class);

    private final RedisProperties redisProperties;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final MapProviderMetrics metrics;
    private final Map<String, AtomicInteger> localCounters = new ConcurrentHashMap<>();

    public MapProviderQuotaGuard(
            RedisProperties redisProperties,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            MapProviderMetrics metrics
    ) {
        this.redisProperties = redisProperties;
        this.redisTemplateProvider = redisTemplateProvider;
        this.metrics = metrics;
    }

    public QuotaSnapshot acquire(
            String provider,
            MapProviderOperation operation,
            int providerDailyLimit,
            int internalUsagePercent
    ) {
        int internalLimit = Math.max(1, Math.floorDiv(providerDailyLimit * internalUsagePercent, 100));
        ZonedDateTime now = ZonedDateTime.now(VIETNAM_ZONE);
        ZonedDateTime nextReset = now.toLocalDate().plusDays(1).atStartOfDay(VIETNAM_ZONE);
        String key = quotaKey(provider, operation, now.toLocalDate());
        long used = redisProperties.isEnabled()
                ? acquireRedis(key, internalLimit, now, nextReset)
                : acquireLocal(key, internalLimit);

        if (used < 0) {
            metrics.updateQuota(provider, operation, 0, internalLimit);
            metrics.quotaExhausted(provider, operation);
            throw new MapRequestLimitException(
                    QUOTA_EXHAUSTED_CODE,
                    "Đã hết lượt tra cứu bản đồ hôm nay.",
                    nextReset.toOffsetDateTime()
            );
        }

        long remaining = Math.max(0L, internalLimit - used);
        metrics.updateQuota(provider, operation, remaining, internalLimit);
        return new QuotaSnapshot(used, remaining, internalLimit, nextReset.toOffsetDateTime());
    }

    private long acquireRedis(
            String key,
            int internalLimit,
            ZonedDateTime now,
            ZonedDateTime nextReset
    ) {
        try {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate == null) {
                throw new IllegalStateException("Redis template is unavailable");
            }
            long ttlSeconds = Math.max(60L, Duration.between(now, nextReset).toSeconds() + 60L);
            Long result = redisTemplate.execute(
                    ACQUIRE_QUOTA_SCRIPT,
                    List.of(key),
                    String.valueOf(internalLimit),
                    String.valueOf(ttlSeconds)
            );
            if (result == null) {
                throw new IllegalStateException("Redis quota script returned no result");
            }
            return result;
        } catch (MapRequestLimitException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ServiceUnavailableException("Dịch vụ kiểm soát hạn mức bản đồ đang tạm thời không khả dụng");
        }
    }

    private long acquireLocal(String key, int internalLimit) {
        AtomicInteger counter = localCounters.computeIfAbsent(key, ignored -> new AtomicInteger());
        while (true) {
            int current = counter.get();
            if (current >= internalLimit) return -1;
            if (counter.compareAndSet(current, current + 1)) return current + 1L;
        }
    }

    static String quotaKey(String provider, MapProviderOperation operation, LocalDate date) {
        return "maps:quota:%s:%s:%s".formatted(
                provider.toLowerCase(Locale.ROOT),
                operation.name().toLowerCase(Locale.ROOT),
                date
        );
    }

    public record QuotaSnapshot(long used, long remaining, long limit, OffsetDateTime retryAfter) {
    }
}
