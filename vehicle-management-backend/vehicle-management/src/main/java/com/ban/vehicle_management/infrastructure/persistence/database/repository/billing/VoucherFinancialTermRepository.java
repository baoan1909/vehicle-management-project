package com.ban.vehicle_management.infrastructure.persistence.database.repository.billing;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.VoucherFinancialTermEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoucherFinancialTermRepository extends JpaRepository<VoucherFinancialTermEntity, UUID> {

    @Query("select t from VoucherFinancialTermEntity t where t.voucherId = :voucherId and t.status = 'ACTIVE'"
            + " and (t.validFrom is null or t.validFrom <= :at) and (t.validTo is null or t.validTo >= :at)"
            + " order by t.settlementDelayDays desc")
    Optional<VoucherFinancialTermEntity> findActiveTerm(@Param("voucherId") UUID voucherId, @Param("at") Instant at);
}
