package com.ban.vehicle_management.infrastructure.persistence.adapter.billing;

import com.ban.vehicle_management.application.billing.platformfee.model.result.PlatformFeeConfigurationResult;
import com.ban.vehicle_management.application.billing.platformfee.port.out.PlatformFeeConfigurationPortOut;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.PlatformFeeConfigurationEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.PlatformFeeConfigurationRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PlatformFeeConfigurationPersistenceAdapter implements PlatformFeeConfigurationPortOut {

    private static final String GLOBAL_KEY = "GLOBAL";

    private final PlatformFeeConfigurationRepository repository;

    public PlatformFeeConfigurationPersistenceAdapter(PlatformFeeConfigurationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PlatformFeeConfigurationResult> findGlobal() {
        return repository.findById(GLOBAL_KEY).map(this::toResult);
    }

    @Override
    public Optional<PlatformFeeConfigurationResult> updateGlobal(
            BigDecimal maxPartnerTotalRate,
            long expectedVersion,
            Instant updatedAt,
            UUID updatedBy
    ) {
        if (repository.updateGlobal(maxPartnerTotalRate, expectedVersion, updatedAt, updatedBy) != 1) {
            return Optional.empty();
        }
        return findGlobal();
    }

    private PlatformFeeConfigurationResult toResult(PlatformFeeConfigurationEntity entity) {
        return new PlatformFeeConfigurationResult(
                entity.getMaxPartnerTotalRate(),
                entity.getRowVersion(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy());
    }
}
