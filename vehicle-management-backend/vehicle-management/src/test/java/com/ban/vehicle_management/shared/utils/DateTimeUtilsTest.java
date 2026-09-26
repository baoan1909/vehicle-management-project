package com.ban.vehicle_management.shared.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DateTimeUtilsTest {

    @BeforeEach
    void configureApplicationZone() {
        DateTimeUtils.configureAppZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    @AfterEach
    void resetApplicationZone() {
        DateTimeUtils.configureAppZone(DateTimeUtils.UTC_ZONE);
    }

    @Test
    void shouldParseIsoInstant() {
        Instant instant = DateTimeUtils.parseIsoInstant("2025-12-15T14:00:00Z");

        assertEquals(Instant.parse("2025-12-15T14:00:00Z"), instant);
    }

    @Test
    void shouldThrowWhenIsoInstantIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> DateTimeUtils.parseIsoInstant("not-an-instant"));
    }

    @Test
    void shouldReturnApplicationZoneDateTimeParts() {
        DateTimeUtils.DateTimeParts parts = DateTimeUtils.toDateTimeParts(Instant.parse("2025-12-15T14:00:00Z"));

        assertEquals("15.12.2025", parts.date());
        assertEquals("21:00", parts.time());
    }

    @Test
    void shouldFormatInstantInApplicationZoneByDefault() {
        String formatted = DateTimeUtils.formatInstant(Instant.parse("2025-12-15T14:00:00Z"));

        assertEquals("21:00 15-12-2025", formatted);
    }

    @Test
    void shouldConvertVietnamLocalDateToStartOfDayInstant() {
        Instant instant = DateTimeUtils.startOfDayInAppZone(LocalDate.of(2025, 12, 15));

        assertEquals(Instant.parse("2025-12-14T17:00:00Z"), instant);
    }

    @Test
    void shouldConvertInstantToVietnamLocalDate() {
        LocalDate localDate = DateTimeUtils.toAppLocalDate(Instant.parse("2025-12-14T17:00:00Z"));

        assertEquals(LocalDate.of(2025, 12, 15), localDate);
    }

    @Test
    void shouldReturnNullForNullValues() {
        assertNull(DateTimeUtils.parseIsoInstant(null));
        assertNull(DateTimeUtils.toDateTimeParts((Instant) null));
        assertEquals("", DateTimeUtils.formatInstant(null));
        assertNull(DateTimeUtils.startOfDayInAppZone(null));
        assertNull(DateTimeUtils.startOfNextDayInAppZone(null));
        assertNull(DateTimeUtils.toAppLocalDate(null));
    }
}


