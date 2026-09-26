package com.ban.vehicle_management.infrastructure.config.time;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ban.vehicle_management.infrastructure.config.JacksonConfig;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import tools.jackson.databind.json.JsonMapper;

class AppInstantHttpSerializationTest {

    @Test
    void shouldSerializeHttpInstantWithConfiguredApplicationOffset() throws Exception {
        assertEquals(
                "\"2026-09-26T14:49:12.4583123+07:00\"",
                serialize("Asia/Ho_Chi_Minh", "2026-09-26T07:49:12.458312300Z")
        );
    }

    @Test
    void shouldChangeHttpOffsetWhenApplicationTimezoneChanges() throws Exception {
        assertEquals(
                "\"2026-09-26T16:49:12.4583123+09:00\"",
                serialize("Asia/Tokyo", "2026-09-26T07:49:12.458312300Z")
        );
    }

    private String serialize(String zoneId, String instant) throws Exception {
        ApplicationTimeProperties properties = new ApplicationTimeProperties(zoneId);
        JsonMapperBuilderCustomizer customizer =
                new JacksonConfig().applicationTimeHttpJsonCustomizer(properties);
        JsonMapper.Builder builder = JsonMapper.builder().findAndAddModules();
        customizer.customize(builder);
        return builder.build().writeValueAsString(Instant.parse(instant));
    }
}
