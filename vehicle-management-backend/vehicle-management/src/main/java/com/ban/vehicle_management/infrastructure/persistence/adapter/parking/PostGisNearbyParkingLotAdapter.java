package com.ban.vehicle_management.infrastructure.persistence.adapter.parking;

import com.ban.vehicle_management.application.parking.parkinglot.model.result.NearbyParkingLotResult;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.NearbyParkingLotPortOut;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PostGisNearbyParkingLotAdapter implements NearbyParkingLotPortOut {

    private static final String FIND_NEARBY_SQL = """
            WITH search_point AS (
                SELECT public.ST_SetSRID(
                    public.ST_MakePoint(CAST(? AS double precision), CAST(? AS double precision)),
                    4326
                )::geography AS location
            )
            SELECT
                pl.parking_lot_id,
                pl.name,
                pl.address_display,
                pl.latitude,
                pl.longitude,
                pl.status,
                public.ST_Distance(pl.location, search_point.location) AS distance_meters
            FROM parking.parking_lots pl
            CROSS JOIN search_point
            WHERE pl.status IN ('ACTIVE', 'SETUP', 'MAINTENANCE')
              AND pl.location IS NOT NULL
              AND public.ST_DWithin(
                    pl.location,
                    search_point.location,
                    CAST(? AS double precision)
              )
            ORDER BY distance_meters ASC, pl.parking_lot_id ASC
            LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public PostGisNearbyParkingLotAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<NearbyParkingLotResult> findNearby(
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal radiusMeters,
            int limit
    ) {
        return jdbcTemplate.query(
                FIND_NEARBY_SQL,
                (resultSet, rowNumber) -> new NearbyParkingLotResult(
                        resultSet.getObject("parking_lot_id", UUID.class),
                        resultSet.getString("name"),
                        resultSet.getString("address_display"),
                        resultSet.getBigDecimal("latitude"),
                        resultSet.getBigDecimal("longitude"),
                        ParkingLotStatus.valueOf(resultSet.getString("status")),
                        resultSet.getDouble("distance_meters")
                ),
                longitude,
                latitude,
                radiusMeters,
                limit
        );
    }
}
