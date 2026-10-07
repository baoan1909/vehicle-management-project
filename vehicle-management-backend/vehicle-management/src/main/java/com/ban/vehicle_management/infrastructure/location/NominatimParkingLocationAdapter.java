package com.ban.vehicle_management.infrastructure.location;

import com.ban.vehicle_management.application.parking.location.model.ParkingLocationSearchResult;
import com.ban.vehicle_management.application.parking.location.port.out.ParkingLocationPortOut;
import com.ban.vehicle_management.infrastructure.cache.RedisProperties;
import com.ban.vehicle_management.shared.exception.MapRequestLimitException;
import com.ban.vehicle_management.shared.exception.ServiceUnavailableException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Timer;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

@Component
public class NominatimParkingLocationAdapter implements ParkingLocationPortOut {
    private static final String PROVIDER = "nominatim";
    private static final String RATE_KEY = "vm:geocoding:nominatim:global-rate";
    private static final AtomicLong LOCAL_NEXT_REQUEST_AT = new AtomicLong();

    private final NominatimProperties properties;
    private final RedisProperties redisProperties;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectMapper objectMapper;
    private final MapProviderQuotaGuard quotaGuard;
    private final MapProviderMetrics metrics;
    private final Semaphore providerBulkhead;
    private final RestClient restClient;

    public NominatimParkingLocationAdapter(
            NominatimProperties properties,
            RedisProperties redisProperties,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            ObjectMapper objectMapper,
            MapProviderQuotaGuard quotaGuard,
            MapProviderMetrics metrics
    ) {
        this.properties = properties;
        this.redisProperties = redisProperties;
        this.redisTemplateProvider = redisTemplateProvider;
        this.objectMapper = objectMapper;
        this.quotaGuard = quotaGuard;
        this.metrics = metrics;
        this.providerBulkhead = new Semaphore(properties.getMaxConcurrentRequests(), true);
        URI baseUri = validateProviderUri(properties);
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getReadTimeout());
        this.restClient = RestClient.builder()
                .baseUrl(baseUri.toString())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, properties.getUserAgent())
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, properties.getAcceptLanguage())
                .build();
    }

    @Override
    public List<ParkingLocationSearchResult> search(String query) {
        String normalized = Normalizer.normalize(query.trim(), Normalizer.Form.NFKC)
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
        return request("search:" + normalized, "/search", builder -> builder
                .queryParam("q", normalized)
                .queryParam("format", "jsonv2")
                .queryParam("limit", 5)
                .queryParam("countrycodes", "vn")
                .queryParam("accept-language", properties.getAcceptLanguage()));
    }

    @Override
    public List<ParkingLocationSearchResult> reverse(BigDecimal latitude, BigDecimal longitude) {
        String cacheIdentity = "reverse:" + latitude.stripTrailingZeros().toPlainString()
                + ":" + longitude.stripTrailingZeros().toPlainString();
        return request(cacheIdentity, "/reverse", builder -> builder
                .queryParam("lat", latitude)
                .queryParam("lon", longitude)
                .queryParam("format", "jsonv2")
                .queryParam("accept-language", properties.getAcceptLanguage()));
    }

    private List<ParkingLocationSearchResult> request(
            String cacheIdentity,
            String path,
            Function<UriBuilder, UriBuilder> uriCustomizer
    ) {
        MapProviderOperation operation = "/reverse".equals(path)
                ? MapProviderOperation.REVERSE
                : MapProviderOperation.FORWARD;
        String cacheKey = "vm:geocoding:nominatim:v1:" + sha256(cacheIdentity);
        List<ParkingLocationSearchResult> cached = readCache(cacheKey);
        if (cached != null) {
            metrics.cacheRequest(PROVIDER, operation, true);
            return cached;
        }
        metrics.cacheRequest(PROVIDER, operation, false);
        if (!providerBulkhead.tryAcquire()) {
            throw new MapRequestLimitException(
                    "MAP_PROVIDER_BUSY",
                    "Dịch vụ tra cứu bản đồ đang bận. Vui lòng thử lại sau.",
                    OffsetDateTime.now(MapProviderQuotaGuard.VIETNAM_ZONE).plusSeconds(1)
            );
        }
        try {
            for (int attempt = 1; attempt <= properties.getMaxAttempts(); attempt++) {
                acquireRatePermit();
                quotaGuard.acquire(
                        PROVIDER,
                        operation,
                        properties.getDailyRequestLimit(),
                        properties.getQuotaUsagePercent()
                );
                Timer.Sample sample = metrics.providerRequestStarted();
                try {
                    String response = readBoundedResponse(path, uriCustomizer);
                    List<ParkingLocationSearchResult> result = parseResponse(path, response);
                    metrics.providerRequestFinished(sample, PROVIDER, operation, "success");
                    writeCache(cacheKey, result);
                    return result;
                } catch (RestClientException | IOException exception) {
                    metrics.providerRequestFinished(sample, PROVIDER, operation, "error");
                    metrics.providerError(PROVIDER, operation, providerFailureReason(exception));
                }
            }
        } finally {
            providerBulkhead.release();
        }
        throw new ServiceUnavailableException("Parking location search service is unavailable");
    }

    private String readBoundedResponse(
            String path,
            Function<UriBuilder, UriBuilder> uriCustomizer
    ) throws IOException {
        return restClient.get()
                .uri(uriBuilder -> uriCustomizer.apply(uriBuilder.path(path)).build())
                .exchange((request, response) -> {
                    String body;
                    try (InputStream responseBody = response.getBody()) {
                        byte[] bytes = responseBody.readNBytes(properties.getMaxResponseBytes() + 1);
                        if (bytes.length > properties.getMaxResponseBytes()) {
                            throw new IOException("provider_response_too_large");
                        }
                        body = new String(bytes, StandardCharsets.UTF_8);
                    }
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new IOException("provider_http_status_" + response.getStatusCode().value());
                    }
                    return body;
                }, false);
    }

    private List<ParkingLocationSearchResult> parseResponse(String path, String response)
            throws com.fasterxml.jackson.core.JsonProcessingException {
        if (response == null || response.isBlank()) {
            return List.of();
        }
        if ("/reverse".equals(path)) {
            NominatimLocation result = objectMapper.readValue(response, NominatimLocation.class);
            return result.toResult() == null ? List.of() : List.of(result.toResult());
        }
        return Arrays.stream(objectMapper.readValue(response, NominatimLocation[].class))
                .map(NominatimLocation::toResult)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private void acquireRatePermit() {
        StringRedisTemplate redis = redisTemplate();
        if (redis != null) {
            long deadline = System.nanoTime() + Duration.ofMillis(1_100).toNanos();
            while (System.nanoTime() < deadline) {
                try {
                    if (Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(
                            RATE_KEY, "1", Duration.ofSeconds(1)))) {
                        return;
                    }
                } catch (RuntimeException ignored) {
                    break;
                }
                pause(50L);
            }
        }
        while (true) {
            long now = System.currentTimeMillis();
            long next = LOCAL_NEXT_REQUEST_AT.get();
            if (now < next) {
                pause(Math.min(next - now, 100L));
            } else if (LOCAL_NEXT_REQUEST_AT.compareAndSet(next, now + 1000L)) {
                return;
            }
        }
    }

    private void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ServiceUnavailableException("Parking location search was interrupted");
        }
    }

    private String providerFailureReason(Exception exception) {
        String message = exception.getMessage();
        if (message != null && message.contains("provider_response_too_large")) return "response_too_large";
        if (exception instanceof java.net.http.HttpTimeoutException
                || hasCause(exception, java.net.http.HttpTimeoutException.class)) return "timeout";
        if (exception instanceof com.fasterxml.jackson.core.JsonProcessingException) return "invalid_response";
        return "request_failed";
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> causeType) {
        Throwable current = throwable;
        while (current != null) {
            if (causeType.isInstance(current)) return true;
            current = current.getCause();
        }
        return false;
    }

    private List<ParkingLocationSearchResult> readCache(String key) {
        StringRedisTemplate redis = redisTemplate();
        if (redis == null) return null;
        try {
            String raw = redis.opsForValue().get(key);
            return raw == null ? null : objectMapper.readValue(raw, new TypeReference<>() { });
        } catch (Exception ignored) {
            return null;
        }
    }

    private void writeCache(String key, List<ParkingLocationSearchResult> value) {
        StringRedisTemplate redis = redisTemplate();
        if (redis == null) return;
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), properties.getCacheTtl());
        } catch (Exception ignored) {
            // Redis is an optimization. Provider calls remain available when it fails.
        }
    }

    private StringRedisTemplate redisTemplate() {
        try {
            return redisProperties.isEnabled() ? redisTemplateProvider.getIfAvailable() : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private URI validateProviderUri(NominatimProperties configuration) {
        URI uri = URI.create(configuration.getBaseUrl());
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || !configuration.getAllowedHosts().contains(uri.getHost().toLowerCase(Locale.ROOT))
                || uri.getUserInfo() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("Nominatim base URL is not in the HTTPS provider allowlist");
        }
        return uri;
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NominatimLocation(String display_name, String lat, String lon) {
        ParkingLocationSearchResult toResult() {
            try {
                if (display_name == null || lat == null || lon == null) return null;
                return new ParkingLocationSearchResult(display_name, new BigDecimal(lat), new BigDecimal(lon));
            } catch (NumberFormatException exception) {
                return null;
            }
        }
    }
}
