package com.ban.vehicle_management.infrastructure.config.time;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record ApplicationTimeProperties(String timeZone) {

    private static final Pattern SAFE_ZONE_ID = Pattern.compile("[A-Za-z0-9_+./-]+");

    public ApplicationTimeProperties {
        if (timeZone == null || timeZone.isBlank()) {
            throw new IllegalArgumentException("app.time-zone must not be blank");
        }

        timeZone = timeZone.trim();
        if (!SAFE_ZONE_ID.matcher(timeZone).matches()) {
            throw new IllegalArgumentException("app.time-zone contains unsupported characters");
        }

        try {
            ZoneId.of(timeZone);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("app.time-zone is not a valid ZoneId: " + timeZone, exception);
        }
    }

    public ZoneId zoneId() {
        return ZoneId.of(timeZone);
    }
}
