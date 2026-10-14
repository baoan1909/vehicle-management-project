package com.ban.vehicle_management.infrastructure.persistence.adapter.billing;

import com.ban.vehicle_management.application.billing.platformfee.port.out.PlatformFeeRuleVersionPortOut;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeRuleVersion;
import com.ban.vehicle_management.infrastructure.mapper.billing.PlatformFeePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.PlatformFeeRuleVersionEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.PlatformFeeRuleVersionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PlatformFeeRuleVersionPersistenceAdapter implements PlatformFeeRuleVersionPortOut {

    private final PlatformFeeRuleVersionRepository repository;
    private final PlatformFeePersistenceMapper mapper;

    public PlatformFeeRuleVersionPersistenceAdapter(
            PlatformFeeRuleVersionRepository repository,
            PlatformFeePersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public PlatformFeeRuleVersion save(PlatformFeeRuleVersion version) {
        PlatformFeeRuleVersionEntity saved = repository.saveAndFlush(mapper.toVersionEntity(version));
        return mapper.toVersionDomain(saved);
    }

    @Override
    public Optional<PlatformFeeRuleVersion> findById(UUID versionId) {
        return repository.findById(versionId).map(mapper::toVersionDomain);
    }

    @Override
    public List<PlatformFeeRuleVersion> findByFeeTypeId(UUID feeTypeId) {
        return repository.findByPlatformFeeTypeIdOrderByVersionAsc(feeTypeId)
                .stream()
                .map(mapper::toVersionDomain)
                .toList();
    }

    @Override
    public List<PlatformFeeRuleVersion> findActiveByFeeTypeId(UUID feeTypeId) {
        return repository
                .findByPlatformFeeTypeIdAndStatus(feeTypeId,
                        com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeRuleVersionStatus.ACTIVE)
                .stream()
                .map(mapper::toVersionDomain)
                .toList();
    }

    @Override
    public Optional<PlatformFeeRuleVersion> findEffectiveVersion(UUID feeTypeId, Instant effectiveAt) {
        Instant resolvedAt = effectiveAt == null ? Instant.now() : effectiveAt;
        List<PlatformFeeRuleVersionEntity> candidates = repository.findCandidatesAt(feeTypeId, resolvedAt);
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapper.toVersionDomain(candidates.get(0)));
    }

    @Override
    public Optional<PlatformFeeRuleVersion> findLatestByFeeTypeId(UUID feeTypeId) {
        return repository.findFirstByPlatformFeeTypeIdOrderByVersionDesc(feeTypeId)
                .map(mapper::toVersionDomain);
    }

    @Override
    public int findMaxVersion(UUID feeTypeId) {
        return repository.findFirstByPlatformFeeTypeIdOrderByVersionDesc(feeTypeId)
                .map(PlatformFeeRuleVersionEntity::getVersion)
                .orElse(0);
    }
}
