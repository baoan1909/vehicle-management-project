package com.ban.vehicle_management.shared.time;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class ApplicationTimeServiceTest {

    @Test
    void shouldUseConfiguredZoneForBusinessDateAndBoundaries() {
        ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
        Instant fixedInstant = Instant.parse("2025-12-14T17:30:00Z");
        ApplicationTimeService service = new ApplicationTimeService(zoneId, Clock.fixed(fixedInstant, zoneId));

        assertEquals(LocalDate.of(2025, 12, 15), service.currentDate());
        assertEquals(LocalDate.of(2025, 12, 15), service.toAppLocalDate(fixedInstant));
        assertEquals(Instant.parse("2025-12-14T17:00:00Z"), service.startOfDay(LocalDate.of(2025, 12, 15)));
        assertEquals(Instant.parse("2025-12-15T17:00:00Z"), service.startOfNextDay(LocalDate.of(2025, 12, 15)));
    }

    @Test
    void shouldHonorDaylightSavingTransitions() {
        ZoneId zoneId = ZoneId.of("America/New_York");
        ApplicationTimeService service = new ApplicationTimeService(zoneId, Clock.system(zoneId));

        assertEquals(Instant.parse("2026-03-08T05:00:00Z"), service.startOfDay(LocalDate.of(2026, 3, 8)));
        assertEquals(Instant.parse("2026-03-09T04:00:00Z"), service.startOfNextDay(LocalDate.of(2026, 3, 8)));
    }
}
