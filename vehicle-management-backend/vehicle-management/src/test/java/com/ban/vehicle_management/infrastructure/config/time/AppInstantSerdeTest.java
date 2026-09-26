package com.ban.vehicle_management.infrastructure.config.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.infrastructure.config.JacksonConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AppInstantSerdeTest {

    @Test
    void shouldSerializeInstantWithConfiguredOffset() throws JsonProcessingException {
        ObjectMapper objectMapper = objectMapper("Asia/Ho_Chi_Minh");

        assertEquals(
                "\"2025-12-15T21:00:00+07:00\"",
                objectMapper.writeValueAsString(Instant.parse("2025-12-15T14:00:00Z"))
        );
    }

    @Test
    void shouldChangeSerializedOffsetWhenConfigurationChanges() throws JsonProcessingException {
        Instant instant = Instant.parse("2025-12-15T14:00:00Z");

        assertEquals("\"2025-12-15T09:00:00-05:00\"", objectMapper("America/New_York").writeValueAsString(instant));
        assertEquals("\"2025-12-15T23:00:00+09:00\"", objectMapper("Asia/Tokyo").writeValueAsString(instant));
    }

    @Test
    void shouldAcceptOffsetAndUtcInputsButRejectLocalTimestamp() throws JsonProcessingException {
        ObjectMapper objectMapper = objectMapper("Asia/Ho_Chi_Minh");

        assertEquals(Instant.parse("2025-12-15T14:00:00Z"), objectMapper.readValue("\"2025-12-15T21:00:00+07:00\"", Instant.class));
        assertEquals(Instant.parse("2025-12-15T14:00:00Z"), objectMapper.readValue("\"2025-12-15T14:00:00Z\"", Instant.class));
        assertThrows(JsonProcessingException.class, () -> objectMapper.readValue("\"2025-12-15T21:00:00\"", Instant.class));
    }

    private ObjectMapper objectMapper(String zoneId) {
        return new JacksonConfig().objectMapper(new ApplicationTimeProperties(zoneId));
    }
}
