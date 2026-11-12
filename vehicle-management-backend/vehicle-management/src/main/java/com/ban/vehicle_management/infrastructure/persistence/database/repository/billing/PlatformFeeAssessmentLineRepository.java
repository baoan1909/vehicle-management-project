package com.ban.vehicle_management.infrastructure.persistence.database.repository.billing;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.PlatformFeeAssessmentLineEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformFeeAssessmentLineRepository
        extends JpaRepository<PlatformFeeAssessmentLineEntity, UUID> {
    List<PlatformFeeAssessmentLineEntity> findByPlatformFeeAssessmentIdOrderByFeeCode(
            UUID platformFeeAssessmentId);
}
