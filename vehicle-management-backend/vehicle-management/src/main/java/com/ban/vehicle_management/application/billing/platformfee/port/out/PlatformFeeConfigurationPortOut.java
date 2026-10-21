package com.ban.vehicle_management.application.billing.platformfee.port.out;

import com.ban.vehicle_management.application.billing.platformfee.model.result.PlatformFeeConfigurationResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PlatformFeeConfigurationPortOut {

    Optional<PlatformFeeConfigurationResult> findGlobal();

    Optional<PlatformFeeConfigurationResult> updateGlobal(
            BigDecimal maxPartnerTotalRate,
            long expectedVersion,
            Instant updatedAt,
            UUID updatedBy
    );
}
