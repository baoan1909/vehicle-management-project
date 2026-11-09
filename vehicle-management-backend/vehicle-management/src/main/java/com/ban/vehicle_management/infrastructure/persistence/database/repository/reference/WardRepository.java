package com.ban.vehicle_management.infrastructure.persistence.database.repository.reference;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.reference.WardEntity;
import java.util.List;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WardRepository extends JpaRepository<WardEntity, String> {
    List<WardEntity> findAllByProvinceCodeOrderByNameAsc(String provinceCode);

    @Query(value = """
            SELECT ward_code
            FROM reference.current_ward_codes_for_point(
                public.ST_SetSRID(
                    public.ST_MakePoint(
                        CAST(:longitude AS double precision),
                        CAST(:latitude AS double precision)
                    ),
                    4326
                )::geography
            )
            """, nativeQuery = true)
    List<String> findCurrentWardCodes(
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude);
}
