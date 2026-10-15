package com.ban.vehicle_management.infrastructure.persistence.database.repository.billing;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.PlatformFeeConfigurationEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlatformFeeConfigurationRepository
        extends JpaRepository<PlatformFeeConfigurationEntity, String> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE PlatformFeeConfigurationEntity configuration
            SET configuration.maxPartnerTotalRate = :maxPartnerTotalRate,
                configuration.rowVersion = configuration.rowVersion + 1,
                configuration.updatedAt = :updatedAt,
                configuration.updatedBy = :updatedBy
            WHERE configuration.configurationKey = 'GLOBAL'
              AND configuration.rowVersion = :expectedVersion
            """)
    int updateGlobal(
            @Param("maxPartnerTotalRate") BigDecimal maxPartnerTotalRate,
            @Param("expectedVersion") long expectedVersion,
            @Param("updatedAt") Instant updatedAt,
            @Param("updatedBy") UUID updatedBy
    );
}
