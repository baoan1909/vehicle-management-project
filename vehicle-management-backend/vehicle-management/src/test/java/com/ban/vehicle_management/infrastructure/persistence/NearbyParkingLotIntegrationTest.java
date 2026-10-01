package com.ban.vehicle_management.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ban.vehicle_management.application.parking.parkinglot.model.result.NearbyParkingLotResult;
import com.ban.vehicle_management.application.parking.parkinglot.port.in.NearbyParkingLotPortIn;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NearbyParkingLotIntegrationTest {

    private static final UUID DEFAULT_ORGANIZATION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000009001");
    private static final BigDecimal ORIGIN_LATITUDE = new BigDecimal("22.8233");
    private static final BigDecimal ORIGIN_LONGITUDE = new BigDecimal("104.9836");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NearbyParkingLotPortIn nearbyParkingLotPortIn;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldApplyBoundaryStatusLocationOrderingAndLimitRules() {
        UUID nearestId = UUID.randomUUID();
        UUID middleId = UUID.randomUUID();
        UUID boundaryId = UUID.randomUUID();
        UUID outsideId = UUID.randomUUID();
        UUID inactiveId = UUID.randomUUID();
        UUID nullLocationId = UUID.randomUUID();

        insertParkingLotAtDistance(nearestId, "ACTIVE", 1_000);
        insertParkingLotAtDistance(middleId, "ACTIVE", 3_000);
        insertParkingLotAtDistance(boundaryId, "ACTIVE", 4_999);
        insertParkingLotAtDistance(outsideId, "ACTIVE", 5_001);
        insertParkingLotAtDistance(inactiveId, "SETUP", 500);
        insertParkingLotWithoutLocation(nullLocationId);

        List<NearbyParkingLotResult> results = nearbyParkingLotPortIn.findNearby(
                ORIGIN_LATITUDE,
                ORIGIN_LONGITUDE,
                new BigDecimal("5"),
                50
        );

        assertEquals(
                List.of(nearestId, middleId, boundaryId),
                results.stream().map(NearbyParkingLotResult::parkingLotId).toList()
        );
        assertEquals(1_000, results.get(0).distanceMeters(), 0.05);
        assertEquals(3_000, results.get(1).distanceMeters(), 0.05);
        assertEquals(4_999, results.get(2).distanceMeters(), 0.05);

        List<NearbyParkingLotResult> limited = nearbyParkingLotPortIn.findNearby(
                ORIGIN_LATITUDE,
                ORIGIN_LONGITUDE,
                new BigDecimal("5"),
                2
        );
        assertEquals(
                List.of(nearestId, middleId),
                limited.stream().map(NearbyParkingLotResult::parkingLotId).toList()
        );
    }

    @Test
    void shouldExposePublicEndpointWithFiveKilometreAndTwentyItemDefaults() throws Exception {
        UUID parkingLotId = UUID.randomUUID();
        insertParkingLotAtDistance(parkingLotId, "ACTIVE", 1_240);
        for (int index = 0; index < 20; index++) {
            insertParkingLotAtDistance(UUID.randomUUID(), "ACTIVE", 1_300 + index * 10);
        }

        mockMvc.perform(get("/api/public/parking-lots/nearby")
                        .queryParam("latitude", ORIGIN_LATITUDE.toPlainString())
                        .queryParam("longitude", ORIGIN_LONGITUDE.toPlainString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(20))
                .andExpect(jsonPath("$.data[0].parkingLotId").value(parkingLotId.toString()))
                .andExpect(jsonPath("$.data[0].name").value("Nearby " + parkingLotId))
                .andExpect(jsonPath("$.data[0].address").value("Nearby integration test"))
                .andExpect(jsonPath("$.data[0].latitude").isNumber())
                .andExpect(jsonPath("$.data[0].longitude").isNumber())
                .andExpect(jsonPath("$.data[0].distanceMeters").value(1_240))
                .andExpect(jsonPath("$.data[0].distanceKm").value(1.24))
                .andExpect(jsonPath("$.data[0].organizationId").doesNotExist());
    }

    @Test
    void shouldRejectInvalidPublicSearchParameters() throws Exception {
        mockMvc.perform(get("/api/public/parking-lots/nearby")
                        .queryParam("longitude", ORIGIN_LONGITUDE.toPlainString()))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/public/parking-lots/nearby")
                        .queryParam("latitude", "91")
                        .queryParam("longitude", ORIGIN_LONGITUDE.toPlainString()))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/public/parking-lots/nearby")
                        .queryParam("latitude", ORIGIN_LATITUDE.toPlainString())
                        .queryParam("longitude", ORIGIN_LONGITUDE.toPlainString())
                        .queryParam("radiusKm", "5.000001"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/public/parking-lots/nearby")
                        .queryParam("latitude", ORIGIN_LATITUDE.toPlainString())
                        .queryParam("longitude", ORIGIN_LONGITUDE.toPlainString())
                        .queryParam("limit", "51"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUsePartialGistIndexInExplainAnalyze() {
        insertParkingLotAtDistance(UUID.randomUUID(), "ACTIVE", 1_000);
        jdbcTemplate.execute("SET LOCAL enable_seqscan = off");

        List<Map<String, Object>> planRows = jdbcTemplate.queryForList("""
                EXPLAIN (ANALYZE, COSTS OFF, SUMMARY OFF)
                SELECT pl.parking_lot_id
                FROM parking.parking_lots pl
                WHERE pl.status = 'ACTIVE'
                  AND pl.location IS NOT NULL
                  AND public.ST_DWithin(
                        pl.location,
                        public.ST_SetSRID(
                            public.ST_MakePoint(CAST(? AS double precision), CAST(? AS double precision)),
                            4326
                        )::geography,
                        CAST(? AS double precision)
                  )
                """, ORIGIN_LONGITUDE, ORIGIN_LATITUDE, 5_000);
        String plan = planRows.stream()
                .map(row -> String.valueOf(row.values().iterator().next()))
                .reduce("", (left, right) -> left + System.lineSeparator() + right);

        assertTrue(plan.contains("idx_parking_lots_active_location_gist"), plan);
    }

    private void insertParkingLotAtDistance(UUID parkingLotId, String status, int distanceMeters) {
        int inserted = jdbcTemplate.update("""
                WITH projected AS (
                    SELECT public.ST_Project(
                        public.ST_SetSRID(
                            public.ST_MakePoint(CAST(? AS double precision), CAST(? AS double precision)),
                            4326
                        )::geography,
                        CAST(? AS double precision),
                        radians(90)
                    ) AS location
                ), prepared AS (
                    SELECT
                        public.ST_Y(location::geometry) AS latitude,
                        public.ST_X(location::geometry) AS longitude,
                        (
                            SELECT ward_code
                            FROM reference.current_ward_codes_for_point(location)
                            LIMIT 1
                        ) AS current_ward_code
                    FROM projected
                )
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
                )
                SELECT ?, ?, ?, ?, 10, ?, latitude, longitude, NULL,
                       'CURRENT', 'Nearby integration test', current_ward_code, 'RESOLVED'
                FROM prepared
                """,
                ORIGIN_LONGITUDE,
                ORIGIN_LATITUDE,
                distanceMeters,
                parkingLotId,
                DEFAULT_ORGANIZATION_ID,
                "NBY-" + parkingLotId,
                "Nearby " + parkingLotId,
                status
        );
        assertEquals(1, inserted);
    }

    private void insertParkingLotWithoutLocation(UUID parkingLotId) {
        int inserted = jdbcTemplate.update("""
                INSERT INTO parking.parking_lots (
                    parking_lot_id,
                    organization_id,
                    code,
                    name,
                    total_capacity,
                    status,
                    address_input_scheme,
                    address_display,
                    geocoding_status
                ) VALUES (?, ?, ?, ?, 10, 'SETUP', 'CURRENT', 'No location', 'NOT_REQUESTED')
                """,
                parkingLotId,
                DEFAULT_ORGANIZATION_ID,
                "NBY-NULL-" + parkingLotId,
                "No location " + parkingLotId
        );
        assertEquals(1, inserted);
    }
}
