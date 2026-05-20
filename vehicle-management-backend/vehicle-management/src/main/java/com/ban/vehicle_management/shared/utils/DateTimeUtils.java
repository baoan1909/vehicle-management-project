package com.ban.vehicle_management.shared.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtils {

    public static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final ZoneId UTC_ZONE = ZoneOffset.UTC;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd-MM-yyyy");

    private DateTimeUtils() {
    }

    public record DateTimeParts(String date, String time) {
    }

    public static Instant parseIsoInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(value.trim());
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid ISO-8601 instant: " + value, exception);
        }
    }

    public static DateTimeParts toDateTimeParts(Instant instant) {
        return toDateTimeParts(instant, UTC_ZONE);
    }

    public static DateTimeParts toDateTimeParts(String instantValue) {
        return toDateTimeParts(parseIsoInstant(instantValue), UTC_ZONE);
    }

    public static DateTimeParts toDateTimeParts(Instant instant, ZoneId zoneId) {
        if (instant == null) {
            return null;
        }

        ZonedDateTime zonedDateTime = instant.atZone(zoneId == null ? UTC_ZONE : zoneId);
        return new DateTimeParts(
                zonedDateTime.format(DATE_FORMATTER),
                zonedDateTime.format(TIME_FORMATTER)
        );
    }

    public static String formatInstant(Instant instant) {
        return formatInstant(instant, UTC_ZONE);
    }

    public static String formatInstant(Instant instant, ZoneId zoneId) {
        if (instant == null) {
            return "";
        }

        ZoneId resolvedZoneId = zoneId == null ? UTC_ZONE : zoneId;
        return instant.atZone(resolvedZoneId).format(DATE_TIME_FORMATTER);
    }

    public static Instant startOfDayInVietnam(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }

        return localDate.atStartOfDay(VIETNAM_ZONE).toInstant();
    }

    public static Instant startOfNextDayInVietnam(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }

        return localDate.plusDays(1).atStartOfDay(VIETNAM_ZONE).toInstant();
    }

    public static LocalDate toVietnamLocalDate(Instant instant) {
        if (instant == null) {
            return null;
        }

        return LocalDate.ofInstant(instant, ZoneOffset.UTC);
    }
}


