package com.ban.vehicle_management.shared.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class ApplicationTimeService {

    private static final DateTimeFormatter DISPLAY_DATE_TIME =
            DateTimeFormatter.ofPattern("HH:mm dd-MM-yyyy");
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final ZoneId zoneId;
    private final Clock clock;

    public ApplicationTimeService(ZoneId zoneId, Clock clock) {
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public ZoneId zoneId() {
        return zoneId;
    }

    public Clock clock() {
        return clock;
    }

    public Instant now() {
        return Instant.now(clock);
    }

    public LocalDate currentDate() {
        return LocalDate.now(clock);
    }

    public LocalDate toAppLocalDate(Instant instant) {
        return instant == null ? null : LocalDate.ofInstant(instant, zoneId);
    }

    public Instant startOfDay(LocalDate localDate) {
        return localDate == null ? null : localDate.atStartOfDay(zoneId).toInstant();
    }

    public Instant startOfNextDay(LocalDate localDate) {
        return localDate == null ? null : localDate.plusDays(1).atStartOfDay(zoneId).toInstant();
    }

    public String formatInstantForDisplay(Instant instant) {
        return instant == null ? "" : instant.atZone(zoneId).format(DISPLAY_DATE_TIME);
    }

    public String formatTimeRange(Instant start, Instant end) {
        String startLabel = start == null ? "?" : start.atZone(zoneId).format(DISPLAY_TIME);
        String endLabel = end == null ? "?" : end.atZone(zoneId).format(DISPLAY_TIME);
        return startLabel + " - " + endLabel;
    }
}
