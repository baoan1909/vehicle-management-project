package com.ban.vehicle_management.shared.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DateTimeUtilsTest {

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
    void shouldReturnUtcDateTimeParts() {
        DateTimeUtils.DateTimeParts parts = DateTimeUtils.toDateTimeParts(Instant.parse("2025-12-15T14:00:00Z"));

        assertEquals("15.12.2025", parts.date());
        assertEquals("14:00", parts.time());
    }

    @Test
    void shouldFormatInstantInUtcByDefault() {
        String formatted = DateTimeUtils.formatInstant(Instant.parse("2025-12-15T14:00:00Z"));

        assertEquals("14:00 15-12-2025", formatted);
    }

    @Test
    void shouldConvertVietnamLocalDateToStartOfDayInstant() {
        Instant instant = DateTimeUtils.startOfDayInVietnam(LocalDate.of(2025, 12, 15));

        assertEquals(Instant.parse("2025-12-14T17:00:00Z"), instant);
    }

    @Test
    void shouldConvertInstantToVietnamLocalDate() {
        LocalDate localDate = DateTimeUtils.toVietnamLocalDate(Instant.parse("2025-12-14T17:00:00Z"));

        assertEquals(LocalDate.of(2025, 12, 15), localDate);
    }

    @Test
    void shouldReturnNullForNullValues() {
        assertNull(DateTimeUtils.parseIsoInstant(null));
        assertNull(DateTimeUtils.toDateTimeParts((Instant) null));
        assertEquals("", DateTimeUtils.formatInstant(null));
        assertNull(DateTimeUtils.startOfDayInVietnam(null));
        assertNull(DateTimeUtils.startOfNextDayInVietnam(null));
        assertNull(DateTimeUtils.toVietnamLocalDate(null));
    }
}


