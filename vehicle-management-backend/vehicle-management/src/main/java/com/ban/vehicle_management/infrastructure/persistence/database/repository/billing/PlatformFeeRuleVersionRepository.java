package com.ban.vehicle_management.infrastructure.persistence.database.repository.billing;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.PlatformFeeRuleVersionEntity;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeRuleVersionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlatformFeeRuleVersionRepository
        extends JpaRepository<PlatformFeeRuleVersionEntity, UUID>,
        JpaSpecificationExecutor<PlatformFeeRuleVersionEntity> {

    List<PlatformFeeRuleVersionEntity> findByPlatformFeeTypeIdOrderByVersionAsc(UUID platformFeeTypeId);

    List<PlatformFeeRuleVersionEntity> findByPlatformFeeTypeIdAndStatus(
            UUID platformFeeTypeId, PlatformFeeRuleVersionStatus status);

    Optional<PlatformFeeRuleVersionEntity> findFirstByPlatformFeeTypeIdOrderByVersionDesc(UUID platformFeeTypeId);

    @Query("""
            SELECT version FROM PlatformFeeRuleVersionEntity version
            WHERE version.platformFeeTypeId = :feeTypeId
              AND version.status = 'ACTIVE'
              AND (version.effectiveFrom IS NULL OR version.effectiveFrom <= :effectiveAt)
              AND (version.effectiveTo IS NULL OR :effectiveAt < version.effectiveTo)
            ORDER BY version.version DESC
            """)
    List<PlatformFeeRuleVersionEntity> findCandidatesAt(
            @Param("feeTypeId") UUID feeTypeId,
            @Param("effectiveAt") java.time.Instant effectiveAt);
}
