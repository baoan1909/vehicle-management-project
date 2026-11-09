package com.ban.vehicle_management.infrastructure.health;

import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingLotRepository;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Verifies that the spatial database capability required by nearby parking-lot
 * searches is installed and that its schema objects were migrated successfully.
 */
@Component("postGis")
public class PostGisHealthIndicator implements HealthIndicator {

    private static final String EXPECTED_LOCATION_TYPE = "geography(Point,4326)";
    private static final String EXPECTED_INDEX = "idx_parking_lots_active_location_gist";

    private final ParkingLotRepository repository;

    public PostGisHealthIndicator(ParkingLotRepository repository) {
        this.repository = repository;
    }

    @Override
    public Health health() {
        try {
            String extensionVersion = repository.findPostGisExtensionVersion();
            if (extensionVersion == null) {
                return Health.down().withDetail("reason", "POSTGIS_EXTENSION_MISSING").build();
            }

            String locationType = repository.findLocationColumnType();
            if (!EXPECTED_LOCATION_TYPE.equalsIgnoreCase(locationType)) {
                return Health.down()
                        .withDetail("reason", "POSTGIS_LOCATION_TYPE_INVALID")
                        .withDetail("actualLocationType", locationType == null ? "missing" : locationType)
                        .withDetail("expectedLocationType", EXPECTED_LOCATION_TYPE)
                        .build();
            }

            String indexName = repository.findActiveLocationSpatialIndex();
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

}
