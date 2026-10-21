package com.ban.vehicle_management.application.billing.platformfee.port.out;

import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeRuleVersion;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeRuleVersionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlatformFeeRuleVersionPortOut {

    PlatformFeeRuleVersion save(PlatformFeeRuleVersion version);

    Optional<PlatformFeeRuleVersion> findById(UUID versionId);

    List<PlatformFeeRuleVersion> findByFeeTypeId(UUID feeTypeId);

    List<PlatformFeeRuleVersion> findActiveByFeeTypeId(UUID feeTypeId);

    Optional<PlatformFeeRuleVersion> findEffectiveVersion(UUID feeTypeId, Instant effectiveAt);

    Optional<PlatformFeeRuleVersion> findLatestByFeeTypeId(UUID feeTypeId);

    int findMaxVersion(UUID feeTypeId);
}
