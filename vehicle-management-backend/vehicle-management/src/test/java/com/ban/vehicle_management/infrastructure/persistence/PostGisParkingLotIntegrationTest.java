package com.ban.vehicle_management.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.infrastructure.health.PgVectorHealthIndicator;
import com.ban.vehicle_management.infrastructure.health.PostGisHealthIndicator;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.health.contributor.Status;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PostGisParkingLotIntegrationTest {

    private static final UUID DEFAULT_ORGANIZATION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000009001");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PgVectorHealthIndicator pgVectorHealthIndicator;

    @Autowired
    private PostGisHealthIndicator postGisHealthIndicator;

    @Test
    void shouldInstallBothRequiredExtensionsAndBackfillEveryCoordinate() {
        List<String> extensions = jdbcTemplate.queryForList("""
                SELECT extname
                FROM pg_extension
                WHERE extname IN ('vector', 'postgis')
                ORDER BY extname
                """, String.class);

        assertEquals(List.of("postgis", "vector"), extensions);
        assertEquals(Status.UP, pgVectorHealthIndicator.health().getStatus());
        assertEquals(Status.UP, postGisHealthIndicator.health().getStatus());
        assertEquals(0, jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM parking.parking_lots
                WHERE latitude IS NOT NULL
                  AND longitude IS NOT NULL
                  AND location IS NULL
                """, Integer.class));
    }

    @Test
    void shouldLoadAllCurrentWardBoundariesAndResolveVietnamPoint() {
        assertEquals(3321, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM reference.gis_wards",
                Integer.class
        ));
        List<String> wardCodes = jdbcTemplate.queryForList("""
                SELECT ward_code
                FROM reference.current_ward_codes_for_point(
                    public.ST_SetSRID(public.ST_MakePoint(106.700806, 10.776889), 4326)::geography
                )
                """, String.class);
        assertEquals(1, wardCodes.size());
    }

    @Test
    void shouldRejectActiveParkingLotOutsideVietnam() {
        assertThrows(DataIntegrityViolationException.class, () -> insertParkingLotWithWard(
                UUID.randomUUID(),
                "ACTIVE",
                decimal("0"),
                decimal("0"),
                "00004"
        ));
    }

    @Test
    void shouldDeriveLocationFromNumericCoordinatesAndIgnoreDirectGeographyInput() {
        UUID parkingLotId = UUID.randomUUID();
        insertParkingLotWithForgedLocation(
                parkingLotId,
                "ACTIVE",
                new BigDecimal("10.776889"),
                new BigDecimal("106.700806")
        );

        Map<String, Object> location = jdbcTemplate.queryForMap("""
                SELECT public.ST_SRID(location) AS srid,
                       public.ST_Y(location::geometry) AS latitude,
                       public.ST_X(location::geometry) AS longitude
                FROM parking.parking_lots
                WHERE parking_lot_id = ?
                """, parkingLotId);

        assertEquals(4326, ((Number) location.get("srid")).intValue());
        assertEquals(10.776889, ((Number) location.get("latitude")).doubleValue(), 0.0000001);
        assertEquals(106.700806, ((Number) location.get("longitude")).doubleValue(), 0.0000001);

        jdbcTemplate.update("""
                UPDATE parking.parking_lots
                SET status = 'SETUP',
                    latitude = ?,
                    longitude = ?,
                    location = public.ST_GeogFromText('SRID=4326;POINT(1 1)')
                WHERE parking_lot_id = ?
                """, decimal("10.790000"), decimal("106.710000"), parkingLotId);

        Map<String, Object> updatedLocation = jdbcTemplate.queryForMap("""
                SELECT public.ST_Y(location::geometry) AS latitude,
                       public.ST_X(location::geometry) AS longitude
                FROM parking.parking_lots
                WHERE parking_lot_id = ?
                """, parkingLotId);
        assertEquals(10.790000, ((Number) updatedLocation.get("latitude")).doubleValue(), 0.0000001);
        assertEquals(106.710000, ((Number) updatedLocation.get("longitude")).doubleValue(), 0.0000001);

        jdbcTemplate.update("""
                UPDATE parking.parking_lots
                SET latitude = NULL, longitude = NULL
                WHERE parking_lot_id = ?
                """, parkingLotId);
        assertTrue(Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT location IS NULL
                FROM parking.parking_lots
                WHERE parking_lot_id = ?
                """, Boolean.class, parkingLotId)));
    }

    @Test
    void shouldRejectAnIncompleteCoordinatePair() {
        assertThrows(DataIntegrityViolationException.class, () -> insertParkingLot(
                UUID.randomUUID(),
                "ACTIVE",
                new BigDecimal("10.776889"),
                null
        ));
    }

    @Test
    void shouldRejectLatitudeOutsideItsValidRange() {
        assertThrows(DataIntegrityViolationException.class, () -> insertParkingLot(
                UUID.randomUUID(),
                "ACTIVE",
                new BigDecimal("90.000001"),
                new BigDecimal("106.700806")
        ));
    }

    @Test
    void shouldRejectLongitudeOutsideItsValidRange() {
        assertThrows(DataIntegrityViolationException.class, () -> insertParkingLot(
                UUID.randomUUID(),
                "ACTIVE",
                new BigDecimal("10.776889"),
                new BigDecimal("180.000001")
        ));
    }

    @Test
    void shouldFindOnlyParkingLotsWithinFiveKilometres() {
        UUID originId = UUID.randomUUID();
        UUID nearbyId = UUID.randomUUID();
        UUID distantId = UUID.randomUUID();
        insertParkingLot(originId, "ACTIVE", decimal("10.776889"), decimal("106.700806"));
        insertParkingLot(nearbyId, "ACTIVE", decimal("10.780100"), decimal("106.709000"));
        insertParkingLot(distantId, "ACTIVE", decimal("10.850000"), decimal("106.800000"));

        List<UUID> result = jdbcTemplate.queryForList("""
                SELECT parking_lot_id
                FROM parking.parking_lots
                WHERE status = 'ACTIVE'
                  AND location IS NOT NULL
                  AND public.ST_DWithin(
                        location,
                        public.ST_SetSRID(public.ST_MakePoint(?, ?), 4326)::geography,
                        ?
                  )
                  AND parking_lot_id IN (?, ?, ?)
                ORDER BY public.ST_Distance(
                        location,
                        public.ST_SetSRID(public.ST_MakePoint(?, ?), 4326)::geography
                )
                """,
                UUID.class,
                106.700806, 10.776889, 5_000,
                originId, nearbyId, distantId,
                106.700806, 10.776889
        );

        assertEquals(List.of(originId, nearbyId), result);
    }

    @Test
    void shouldUseThePartialGistIndexForActiveNearbySearch() {
        insertParkingLot(
                UUID.randomUUID(),
                "ACTIVE",
                decimal("10.776889"),
                decimal("106.700806")
        );
        jdbcTemplate.execute("SET LOCAL enable_seqscan = off");

        List<Map<String, Object>> planRows = jdbcTemplate.queryForList("""
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
                """);
        String plan = planRows.stream()
                .map(row -> String.valueOf(row.values().iterator().next()))
                .reduce("", (left, right) -> left + System.lineSeparator() + right);

        assertTrue(plan.contains("idx_parking_lots_active_location_gist"), plan);
    }

    private BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }

    private void insertParkingLot(
            UUID parkingLotId,
            String status,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        String sql = """
                INSERT INTO parking.parking_lots (
                    parking_lot_id,
                    organization_id,
                    code,
                    name,
                    total_capacity,
                    status,
                    latitude,
                    longitude,
                    location,
                    address_input_scheme,
                    address_display,
                    current_ward_code,
                    geocoding_status
                ) VALUES (?, ?, ?, ?, 10, ?, ?, ?, NULL, 'CURRENT', 'PostGIS integration test', ?, 'RESOLVED')
                """;
        updateParkingLot(sql, parkingLotId, status, latitude, longitude, resolveWardCode(latitude, longitude));
    }

    private void insertParkingLotWithForgedLocation(
            UUID parkingLotId,
            String status,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        String sql = """
                INSERT INTO parking.parking_lots (
                    parking_lot_id,
                    organization_id,
                    code,
                    name,
                    total_capacity,
                    status,
                    latitude,
                    longitude,
                    location,
                    address_input_scheme,
                    address_display,
                    current_ward_code,
                    geocoding_status
                ) VALUES (?, ?, ?, ?, 10, ?, ?, ?, public.ST_GeogFromText('SRID=4326;POINT(0 0)'), 'CURRENT', 'PostGIS integration test', ?, 'RESOLVED')
                """;
        updateParkingLot(sql, parkingLotId, status, latitude, longitude, resolveWardCode(latitude, longitude));
    }

    private void insertParkingLotWithWard(
            UUID parkingLotId,
            String status,
            BigDecimal latitude,
            BigDecimal longitude,
            String wardCode
    ) {
        String sql = """
                INSERT INTO parking.parking_lots (
                    parking_lot_id, organization_id, code, name, total_capacity,
                    status, latitude, longitude, location, address_input_scheme,
                    address_display, current_ward_code, geocoding_status
                ) VALUES (?, ?, ?, ?, 10, ?, ?, ?, NULL, 'CURRENT',
                    'PostGIS integration test', ?, 'RESOLVED')
                """;
        updateParkingLot(sql, parkingLotId, status, latitude, longitude, wardCode);
    }

    private void updateParkingLot(
            String sql,
            UUID parkingLotId,
            String status,
            BigDecimal latitude,
            BigDecimal longitude,
            String wardCode
    ) {
        jdbcTemplate.update(
                sql,
                parkingLotId,
                DEFAULT_ORGANIZATION_ID,
                "POSTGIS-" + parkingLotId,
                "PostGIS integration test",
                status,
                latitude,
                longitude,
                wardCode
        );
    }

    private String resolveWardCode(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null
                || latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(BigDecimal.valueOf(90)) > 0
                || longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            return "00004";
        }
        return jdbcTemplate.queryForList("""
                        SELECT ward_code
                        FROM reference.current_ward_codes_for_point(
                            public.ST_SetSRID(public.ST_MakePoint(?, ?), 4326)::geography
                        )
                        LIMIT 1
                        """, String.class, longitude, latitude)
                .stream()
                .findFirst()
                .orElse("00004");
    }
}
