package com.ban.vehicle_management.infrastructure.location;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.infrastructure.cache.RedisProperties;
import com.ban.vehicle_management.shared.exception.ServiceUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

class NominatimParkingLocationAdapterTest {

    @Test
    void shouldRejectProviderOutsideFixedHttpsAllowlist() {
        NominatimProperties properties = new NominatimProperties();
        properties.setBaseUrl("https://169.254.169.254/latest/meta-data");

        assertThrows(IllegalArgumentException.class, () -> new NominatimParkingLocationAdapter(
                properties,
                new RedisProperties(),
                emptyRedisProvider(),
                new ObjectMapper(),
                mock(MapProviderQuotaGuard.class),
                mock(MapProviderMetrics.class)
        ));
    }

    @Test
    void shouldRequireContactAndPositiveDailyLimit() {
        NominatimProperties properties = new NominatimProperties();
        assertTrue(properties.isValid());

        properties.setUserAgent("CoParking/1.0");
        assertFalse(properties.isValid());

        properties.setUserAgent("CoParking/1.0 (contact: dev@coparking.local)");
        properties.setDailyRequestLimit(0);
        assertFalse(properties.isValid());

        properties.setDailyRequestLimit(1_000);
        properties.setQuotaUsagePercent(96);
        assertFalse(properties.isValid());
    }

    @Test
    void shouldReadEmptyJsonArrayFromRawResponseStream() throws Exception {
        NominatimProperties properties = new NominatimProperties();
        NominatimParkingLocationAdapter adapter = adapter(properties);
        stubResponse(adapter, HttpStatus.OK, "[]");

        assertTrue(adapter.search("25/11A, Bùi Xuân Phái, Phường Tây Thạnh").isEmpty());
    }

    @Test
    void shouldRejectProviderResponseLargerThanConfiguredLimit() throws Exception {
        NominatimProperties properties = new NominatimProperties();
        properties.setMaxAttempts(1);
        properties.setMaxResponseBytes(1_024);
        NominatimParkingLocationAdapter adapter = adapter(properties);
        stubResponse(adapter, HttpStatus.OK, "x".repeat(1_025));

        assertThrows(ServiceUnavailableException.class, () -> adapter.search("Tây Thạnh"));
    }

    @Test
    void shouldConvertProviderHttpErrorToServiceUnavailable() throws Exception {
        NominatimProperties properties = new NominatimProperties();
        properties.setMaxAttempts(1);
        NominatimParkingLocationAdapter adapter = adapter(properties);
        stubResponse(adapter, HttpStatus.TOO_MANY_REQUESTS, "{}");

        assertThrows(ServiceUnavailableException.class, () -> adapter.search("Tây Thạnh"));
    }

    private NominatimParkingLocationAdapter adapter(NominatimProperties properties) {
        RedisProperties redisProperties = new RedisProperties();
        redisProperties.setEnabled(false);
        MapProviderMetrics metrics = new MapProviderMetrics(new SimpleMeterRegistry());
        return new NominatimParkingLocationAdapter(
                properties,
                redisProperties,
                emptyRedisProvider(),
                new ObjectMapper(),
                mock(MapProviderQuotaGuard.class),
                metrics
        );
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void stubResponse(
            NominatimParkingLocationAdapter adapter,
            HttpStatus status,
            String body
    ) throws Exception {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec request = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response =
                mock(RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse.class);

        when(restClient.get()).thenReturn(request);
        when(request.uri(any(Function.class))).thenReturn(request);
        when(response.getStatusCode()).thenReturn(status);
        when(response.getBody()).thenReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
        when(request.exchange(any(RestClient.RequestHeadersSpec.ExchangeFunction.class), eq(false)))
                .thenAnswer(invocation -> {
                    RestClient.RequestHeadersSpec.ExchangeFunction exchangeFunction = invocation.getArgument(0);
                    return exchangeFunction.exchange(mock(HttpRequest.class), response);
                });

        ReflectionTestUtils.setField(adapter, "restClient", restClient);
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<StringRedisTemplate> emptyRedisProvider() {
        return mock(ObjectProvider.class);
    }
}
