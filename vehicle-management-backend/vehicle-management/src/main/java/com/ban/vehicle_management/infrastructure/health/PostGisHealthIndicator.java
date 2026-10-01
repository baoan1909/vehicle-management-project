package com.ban.vehicle_management.infrastructure.health;

import java.util.List;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Verifies that the spatial database capability required by nearby parking-lot
 * searches is installed and that its schema objects were migrated successfully.
 */
@Component("postGis")
public class PostGisHealthIndicator implements HealthIndicator {

    private static final String EXPECTED_LOCATION_TYPE = "geography(Point,4326)";
    private static final String EXPECTED_INDEX = "idx_parking_lots_active_location_gist";

    private final JdbcTemplate jdbcTemplate;

    public PostGisHealthIndicator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Health health() {
        try {
            String extensionVersion = singleValue(
                    "SELECT extversion FROM pg_extension WHERE extname = 'postgis'");
            if (extensionVersion == null) {
                return Health.down().withDetail("reason", "POSTGIS_EXTENSION_MISSING").build();
            }

            String locationType = singleValue("""
                    SELECT format_type(attribute.atttypid, attribute.atttypmod)
                    FROM pg_attribute attribute
                    JOIN pg_class relation ON relation.oid = attribute.attrelid
                    JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
                    WHERE namespace.nspname = 'parking'
                      AND relation.relname = 'parking_lots'
                      AND attribute.attname = 'location'
                      AND NOT attribute.attisdropped
                    """);
            if (!EXPECTED_LOCATION_TYPE.equalsIgnoreCase(locationType)) {
                return Health.down()
                        .withDetail("reason", "POSTGIS_LOCATION_TYPE_INVALID")
                        .withDetail("actualLocationType", locationType == null ? "missing" : locationType)
                        .withDetail("expectedLocationType", EXPECTED_LOCATION_TYPE)
                        .build();
            }

            String indexName = singleValue("""
                    SELECT indexname
                    FROM pg_indexes
                    WHERE schemaname = 'parking'
                      AND tablename = 'parking_lots'
                      AND indexname = 'idx_parking_lots_active_location_gist'
                    """);
            if (!EXPECTED_INDEX.equals(indexName)) {
                return Health.down().withDetail("reason", "POSTGIS_SPATIAL_INDEX_MISSING").build();
            }

            return Health.up()
                    .withDetail("extensionVersion", extensionVersion)
                    .withDetail("locationType", locationType)
                    .withDetail("spatialIndex", indexName)
                    .build();
        } catch (Exception exception) {
            return Health.down()
                    .withDetail("reason", "POSTGIS_HEALTH_CHECK_FAILED")
                    .withException(exception)
                    .build();
        }
    }

    private String singleValue(String sql) {
        List<String> values = jdbcTemplate.query(sql, (resultSet, rowNumber) -> resultSet.getString(1));
        return values.isEmpty() ? null : values.getFirst();
    }
}
