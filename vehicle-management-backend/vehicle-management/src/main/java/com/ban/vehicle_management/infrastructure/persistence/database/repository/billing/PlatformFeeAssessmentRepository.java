package com.ban.vehicle_management.infrastructure.persistence.database.repository.billing;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.PlatformFeeAssessmentEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformFeeAssessmentRepository extends JpaRepository<PlatformFeeAssessmentEntity, UUID> {
    Optional<PlatformFeeAssessmentEntity> findByPaymentId(UUID paymentId);
}
