package com.ban.vehicle_management.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class SpatialFreshDatabaseContainerTest {

    private static final String IMAGE = System.getenv().getOrDefault(
            "POSTGRES_SPATIAL_TEST_IMAGE",
            "vehicle-management/postgres-pgvector-postgis:17"
    );

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse(IMAGE).asCompatibleSubstituteFor("postgres")
    ).withDatabaseName("vehicle_management_fresh")
            .withUsername("migration_owner")
            .withPassword("migration_password");

    @BeforeAll
    static void migrateFreshDatabase() {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .defaultSchema("public")
                .schemas("public")
                .validateMigrationNaming(true)
                .cleanDisabled(true)
                .load();

        flyway.migrate();
        assertEquals(0, flyway.info().pending().length);
    }

    @Test
    void shouldInstallExtensionsAndSeedBothAdministrativeDatasets() throws SQLException {
        try (Connection connection = connection()) {
            assertEquals(2, scalar(connection, "SELECT count(*) FROM pg_extension WHERE extname IN ('vector', 'postgis')"));
            assertEquals(34, scalar(connection, "SELECT count(*) FROM reference.provinces"));
            assertEquals(3_321, scalar(connection, "SELECT count(*) FROM reference.wards"));
            assertEquals(63, scalar(connection, "SELECT count(*) FROM reference.legacy_provinces"));
            assertEquals(696, scalar(connection, "SELECT count(*) FROM reference.legacy_districts"));
            assertEquals(10_035, scalar(connection, "SELECT count(*) FROM reference.legacy_wards"));
            assertEquals(3_321, scalar(connection, "SELECT count(*) FROM reference.gis_wards"));
        }
    }

    @Test
    void shouldEnforceReferenceForeignKeys() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            assertThrows(SQLException.class, () -> statement.executeUpdate("""
                    INSERT INTO reference.legacy_districts (
                        code, province_code, name, full_name, code_name,
                        administrative_unit_id, administrative_unit_name
                    ) VALUES ('999', '99', 'Invalid', 'Invalid', 'invalid-test', 1, 'Invalid')
                    """));
        }
    }

    @Test
    void shouldKeepOnlyCanonicalDisplayAddressColumns() throws SQLException {
        try (Connection connection = connection()) {
            assertEquals(0, scalar(connection, """
                    SELECT count(*)
                    FROM information_schema.columns
                    WHERE table_schema IN ('people', 'iam', 'parking')
                      AND table_name IN ('user_profiles', 'organizations', 'parking_lots')
                      AND column_name = 'address'
                    """));
            assertEquals(3, scalar(connection, """
                    SELECT count(*)
                    FROM information_schema.columns
                    WHERE table_schema IN ('people', 'iam', 'parking')
                      AND table_name IN ('user_profiles', 'organizations', 'parking_lots')
                      AND column_name = 'address_display'
                    """));
        }
    }

    @Test
    void shouldSynchronizeLocationRunSpatialQueryAndUseGistIndex() throws SQLException {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID outside = UUID.randomUUID();
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            insertActiveParkingLot(statement, first, 106.700806, 10.776889);
            insertActiveParkingLot(statement, second, 106.709000, 10.780100);
            insertActiveParkingLot(statement, outside, 106.800000, 10.850000);

            assertEquals(3, scalar(connection, """
                    SELECT count(*) FROM parking.parking_lots
                    WHERE latitude IS NOT NULL
                      AND location IS NOT NULL
                      AND public.ST_SRID(location) = 4326
                    """));
            assertEquals(2, scalar(connection, """
                    SELECT count(*) FROM parking.parking_lots
                    WHERE status = 'ACTIVE'
                      AND public.ST_DWithin(
                          location,
                          public.ST_SetSRID(public.ST_MakePoint(106.700806, 10.776889), 4326)::geography,
                          5000
                      )
                    """));

            statement.execute("SET enable_seqscan = off");
            StringBuilder plan = new StringBuilder();
            try (ResultSet resultSet = statement.executeQuery("""
                    EXPLAIN (COSTS OFF)
                    SELECT parking_lot_id
                    FROM parking.parking_lots
                    WHERE status = 'ACTIVE'
                      AND location IS NOT NULL
                      AND public.ST_DWithin(
                          location,
                          public.ST_SetSRID(public.ST_MakePoint(106.700806, 10.776889), 4326)::geography,
                          5000
                      )
                    """)) {
                while (resultSet.next()) {
                    plan.append(resultSet.getString(1)).append(System.lineSeparator());
                }
            }
            assertTrue(plan.toString().contains("idx_parking_lots_active_location_gist"), plan.toString());
        }
    }

    @Test
    void shouldInstallParkingSpaceFoundationConstraints() throws SQLException {
        try (Connection connection = connection()) {
            assertEquals(15, scalar(connection, """
                    SELECT count(*)
                    FROM pg_constraint
                    WHERE conname IN (
                        'fk_parking_sessions_space',
                        'ck_parking_levels_canvas_positive',
                        'ck_parking_levels_floor_height_positive',
                        'ck_parking_spaces_geometry_complete',
                        'ck_parking_spaces_geometry_non_negative',
                        'ck_parking_spaces_rotation_range',
                        'ck_parking_space_layout_items_geometry_positive',
                        'ck_parking_space_layout_items_position_non_negative',
                        'ck_parking_space_layout_items_rotation_range',
                        'ex_parking_space_allocations_no_overlap',
                        'fk_parking_space_allocations_space',
                        'fk_parking_space_allocations_subscription',
                        'uq_parking_levels_lot_code',
                        'uq_parking_spaces_zone_code',
                        'uq_parking_space_layout_items_version_space'
                    )
                    """));
            assertEquals(7, scalar(connection, """
                    SELECT count(*)
                    FROM pg_trigger
                    WHERE NOT tgisinternal
                      AND tgname IN (
                        'trg_zones_check_level_topology',
                        'trg_layout_items_check_topology',
                        'trg_space_allocations_check_topology',
                        'trg_parking_sessions_check_space_topology',
                        'trg_parking_spaces_prevent_referenced_zone_move',
                        'trg_parking_spaces_set_updated_at',
                        'trg_parking_space_allocations_set_updated_at'
                      )
                    """));
            assertEquals(3, scalar(connection, """
                    SELECT count(*)
                    FROM pg_constraint
                    WHERE conname IN (
                        'fk_parking_space_allocations_space',
                        'fk_parking_space_allocations_subscription',
                        'fk_parking_space_layout_items_space'
                    )
                      AND confdeltype = 'r'
                    """));
        }
    }

    @Test
    void shouldRejectInvalidParkingGeometryAndCrossZoneLayoutItem() throws SQLException {
        UUID lotId = UUID.randomUUID();
        UUID levelId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        UUID otherZoneId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        UUID otherSpaceId = UUID.randomUUID();
        UUID layoutVersionId = UUID.randomUUID();
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            insertActiveParkingLot(statement, lotId, 106.700806, 10.776889);
            statement.executeUpdate("""
                    INSERT INTO parking.parking_levels (
                        parking_level_id, parking_lot_id, code, name, canvas_width, canvas_height, status
                    ) VALUES ('%s', '%s', 'L1', 'Level 1', 100, 100, 'ACTIVE')
                    """.formatted(levelId, lotId));
            statement.executeUpdate("""
                    INSERT INTO parking.zones (
                        zone_id, parking_lot_id, parking_level_id, code, name, capacity, status, tracking_mode
                    ) VALUES
                        ('%s', '%s', '%s', 'Z1', 'Zone 1', 0, 'ACTIVE', 'SPACE'),
                        ('%s', '%s', '%s', 'Z2', 'Zone 2', 0, 'ACTIVE', 'SPACE')
                    """.formatted(zoneId, lotId, levelId, otherZoneId, lotId, levelId));
            statement.executeUpdate("""
                    INSERT INTO parking.parking_spaces (
                        parking_space_id, zone_id, code, status, lifecycle_status, status_source, version, rotation
                    ) VALUES
                        ('%s', '%s', 'A-01', 'AVAILABLE', 'ACTIVE', 'MANUAL', 0, 0),
                        ('%s', '%s', 'B-01', 'AVAILABLE', 'ACTIVE', 'MANUAL', 0, 0)
                    """.formatted(spaceId, zoneId, otherSpaceId, otherZoneId));

            assertThrows(SQLException.class, () -> statement.executeUpdate("""
                    INSERT INTO parking.parking_spaces (
                        parking_space_id, zone_id, code, status, lifecycle_status, status_source,
                        version, x, y, width, height, rotation
                    ) VALUES (gen_random_uuid(), '%s', 'INVALID', 'AVAILABLE', 'ACTIVE', 'MANUAL',
                        0, -1, 0, 2, 2, 0)
                    """.formatted(zoneId)));

            statement.executeUpdate("""
                    INSERT INTO parking.parking_layout_versions (layout_version_id, zone_id, version, status)
                    VALUES ('%s', '%s', 1, 'DRAFT')
                    """.formatted(layoutVersionId, zoneId));
            assertThrows(SQLException.class, () -> statement.executeUpdate("""
                    INSERT INTO parking.parking_space_layout_items (
                        layout_item_id, layout_version_id, parking_space_id, x, y, width, height, rotation
                    ) VALUES (gen_random_uuid(), '%s', '%s', 1, 1, 2, 2, 0)
                    """.formatted(layoutVersionId, otherSpaceId)));

            statement.executeUpdate("DELETE FROM parking.parking_lots WHERE parking_lot_id = '%s'".formatted(lotId));
        }
    }

    private static void insertActiveParkingLot(
            Statement statement,
            UUID parkingLotId,
            double longitude,
            double latitude
    ) throws SQLException {
        statement.executeUpdate("""
                INSERT INTO parking.parking_lots (
                    parking_lot_id, organization_id, code, name, total_capacity, status,
                    latitude, longitude, location, address_input_scheme, address_display,
                    current_ward_code, geocoding_status
                )
                SELECT
                    '%s', '00000000-0000-0000-0000-000000009001', '%s', 'Fresh container test', 10, 'ACTIVE',
                    %s, %s, NULL, 'CURRENT', 'Fresh container test', ward_code, 'RESOLVED'
                FROM reference.current_ward_codes_for_point(
                    public.ST_SetSRID(public.ST_MakePoint(%s, %s), 4326)::geography
                )
                LIMIT 1
                """.formatted(parkingLotId, "FRESH-" + parkingLotId, latitude, longitude, longitude, latitude));
    }

    private static int scalar(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next());
            return resultSet.getInt(1);
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
