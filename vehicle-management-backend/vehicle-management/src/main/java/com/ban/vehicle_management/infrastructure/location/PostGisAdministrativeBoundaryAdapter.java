package com.ban.vehicle_management.infrastructure.location;

import com.ban.vehicle_management.application.parking.parkinglot.port.out.AdministrativeBoundaryPortOut;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PostGisAdministrativeBoundaryAdapter implements AdministrativeBoundaryPortOut {

    private static final String FIND_WARDS_SQL = """
            SELECT ward_code
            FROM reference.current_ward_codes_for_point(
                public.ST_SetSRID(public.ST_MakePoint(?, ?), 4326)::geography
            )
            """;

    private final JdbcTemplate jdbcTemplate;

    public PostGisAdministrativeBoundaryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<String> findCurrentWardCodes(BigDecimal latitude, BigDecimal longitude) {
        return jdbcTemplate.queryForList(
                FIND_WARDS_SQL,
                String.class,
                longitude.doubleValue(),
                latitude.doubleValue()
        );
    }
}
