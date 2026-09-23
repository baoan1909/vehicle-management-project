package com.ban.vehicle_management.infrastructure.health;

import java.util.List;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Reports whether accent-insensitive lexical search is backed by the real
 * {@code unaccent} extension or by the SQL translate() fallback inside
 * {@code ai.immutable_unaccent()}.
 */
@Component("unaccent")
public class UnaccentHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;

    public UnaccentHealthIndicator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Health health() {
        try {
            String extensionVersion = singleValue(
                    "SELECT extversion FROM pg_extension WHERE extname = 'unaccent'");
            String wrapper = singleValue("""
                    SELECT proname
                    FROM pg_proc procedure
                    JOIN pg_namespace namespace ON namespace.oid = procedure.pronamespace
                    WHERE namespace.nspname = 'ai'
                      AND procedure.proname = 'immutable_unaccent'
                    """);
            if (wrapper == null) {
                return Health.down().withDetail("reason", "UNACCENT_WRAPPER_MISSING").build();
            }
            if (extensionVersion == null) {
                return Health.up()
                        .withDetail("mode", "TRANSLATE_FALLBACK")
                        .withDetail("reason", "UNACCENT_EXTENSION_MISSING")
                        .build();
            }
            return Health.up()
                    .withDetail("mode", "EXTENSION")
                    .withDetail("extensionVersion", extensionVersion)
                    .build();
        } catch (Exception exception) {
            return Health.down()
                    .withDetail("reason", "UNACCENT_HEALTH_CHECK_FAILED")
                    .withException(exception)
                    .build();
        }
    }

    public void assertExtensionReady() {
        Health health = health();
        if (!Status.UP.equals(health.getStatus())
                || !"EXTENSION".equals(health.getDetails().get("mode"))) {
            throw new IllegalStateException("unaccent chưa sẵn sàng: " + health.getDetails());
        }
    }

    private String singleValue(String sql) {
        List<String> values = jdbcTemplate.query(sql, (resultSet, rowNumber) -> resultSet.getString(1));
        return values.isEmpty() ? null : values.getFirst();
    }
}
