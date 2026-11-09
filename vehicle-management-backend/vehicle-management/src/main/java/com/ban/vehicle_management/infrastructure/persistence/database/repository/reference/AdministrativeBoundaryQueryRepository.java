package com.ban.vehicle_management.infrastructure.persistence.database.repository.reference;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdministrativeBoundaryQueryRepository {

    private static final String FIND_WARDS_SQL = """
            SELECT ward_code
            FROM reference.current_ward_codes_for_point(
                public.ST_SetSRID(public.ST_MakePoint(?, ?), 4326)::geography
            )
            """;

    private final JdbcTemplate jdbcTemplate;

    public AdministrativeBoundaryQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> findCurrentWardCodes(BigDecimal latitude, BigDecimal longitude) {
        return jdbcTemplate.queryForList(
                FIND_WARDS_SQL,
                String.class,
                longitude.doubleValue(),
                latitude.doubleValue()
        );
    }
}
