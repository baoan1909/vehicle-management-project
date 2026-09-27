package com.ban.vehicle_management.shared.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtils {

    public static final ZoneId UTC_ZONE = ZoneOffset.UTC;
    private static volatile ZoneId appZone = UTC_ZONE;
    @Deprecated(forRemoval = false)
    public static volatile ZoneId VIETNAM_ZONE = appZone;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd-MM-yyyy");

    private DateTimeUtils() {
    }

    public static void configureAppZone(ZoneId zoneId) {
        if (zoneId == null) {
            throw new IllegalArgumentException("Application ZoneId must not be null");
        }
        appZone = zoneId;
        VIETNAM_ZONE = zoneId;
    }

    public static ZoneId getAppZone() {
        return appZone;
    }

    public record DateTimeParts(String date, String time) {
    }

    public static Instant parseIsoInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(value.trim()).toInstant();
        } catch (Exception exception) {
            try {
                return Instant.parse(value.trim());
            } catch (Exception fallbackException) {
                throw new IllegalArgumentException("Invalid ISO-8601 instant: " + value, fallbackException);
            }
        }
    }

    public static DateTimeParts toDateTimeParts(Instant instant) {
        return toDateTimeParts(instant, appZone);
    }

    public static DateTimeParts toDateTimeParts(String instantValue) {
        return toDateTimeParts(parseIsoInstant(instantValue), appZone);
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
        return formatInstant(instant, appZone);
    }

    public static String formatInstant(Instant instant, ZoneId zoneId) {
        if (instant == null) {
            return "";
        }

        ZoneId resolvedZoneId = zoneId == null ? appZone : zoneId;
        return instant.atZone(resolvedZoneId).format(DATE_TIME_FORMATTER);
    }

    public static String formatTimeRange(Instant start, Instant end) {
        String startLabel = start == null ? "?" : start.atZone(appZone).format(TIME_FORMATTER);
        String endLabel = end == null ? "?" : end.atZone(appZone).format(TIME_FORMATTER);
        return startLabel + " - " + endLabel;
    }

    public static Instant startOfDayInAppZone(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }

        return localDate.atStartOfDay(appZone).toInstant();
    }

    public static Instant startOfNextDayInAppZone(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }

        return localDate.plusDays(1).atStartOfDay(appZone).toInstant();
    }

    public static LocalDate toAppLocalDate(Instant instant) {
        if (instant == null) {
            return null;
        }

        return LocalDate.ofInstant(instant, appZone);
    }

    @Deprecated(forRemoval = false)
    public static Instant startOfDayInVietnam(LocalDate localDate) {
        return startOfDayInAppZone(localDate);
    }

    @Deprecated(forRemoval = false)
    public static Instant startOfNextDayInVietnam(LocalDate localDate) {
        return startOfNextDayInAppZone(localDate);
    }

    @Deprecated(forRemoval = false)
    public static LocalDate toVietnamLocalDate(Instant instant) {
        return toAppLocalDate(instant);
    }
}


