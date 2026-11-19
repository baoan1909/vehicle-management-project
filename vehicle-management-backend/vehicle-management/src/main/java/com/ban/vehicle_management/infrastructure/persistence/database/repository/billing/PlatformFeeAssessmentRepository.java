package com.ban.vehicle_management.infrastructure.persistence.database.repository.billing;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.PlatformFeeAssessmentEntity;
import java.util.Optional;
import java.util.Collection;
import java.util.UUID;
import java.time.Instant;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlatformFeeAssessmentRepository extends JpaRepository<PlatformFeeAssessmentEntity, UUID> {
    Optional<PlatformFeeAssessmentEntity> findByPaymentId(UUID paymentId);

    @Query(value = """
            select coalesce(sum(assessment.total_fee_amount), 0)
              from billing.platform_fee_assessments assessment
              join billing.invoices invoice on invoice.invoice_id = assessment.invoice_id
             where assessment.status = 'APPLIED'
               and assessment.applied_at >= :from and assessment.applied_at < :to
               and invoice.parking_lot_id in (:parkingLotIds)
            """, nativeQuery = true)
    BigDecimal sumAppliedFeeForParkingLots(
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("parkingLotIds") Collection<UUID> parkingLotIds);
}
