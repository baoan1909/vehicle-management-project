package com.ban.vehicle_management.infrastructure.config.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class ApplicationTimePropertiesTest {

    @Test
    void shouldResolveValidIanaZone() {
        ApplicationTimeProperties properties = new ApplicationTimeProperties(" Asia/Ho_Chi_Minh ");

        assertEquals("Asia/Ho_Chi_Minh", properties.timeZone());
        assertEquals(ZoneId.of("Asia/Ho_Chi_Minh"), properties.zoneId());
    }

    @Test
    void shouldRejectBlankOrInvalidZone() {
        assertThrows(IllegalArgumentException.class, () -> new ApplicationTimeProperties(" "));
        assertThrows(IllegalArgumentException.class, () -> new ApplicationTimeProperties("Invalid/Zone"));
        assertThrows(IllegalArgumentException.class, () -> new ApplicationTimeProperties("UTC'; DROP TABLE users; --"));
    }
}
