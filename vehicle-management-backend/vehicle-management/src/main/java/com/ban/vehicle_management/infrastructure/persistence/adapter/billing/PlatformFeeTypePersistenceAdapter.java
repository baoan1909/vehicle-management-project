package com.ban.vehicle_management.infrastructure.persistence.adapter.billing;

import com.ban.vehicle_management.application.billing.platformfee.model.result.PlatformFeePageResult;
import com.ban.vehicle_management.application.billing.platformfee.port.out.PlatformFeeTypePortOut;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeType;
import com.ban.vehicle_management.infrastructure.mapper.billing.PlatformFeePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.PlatformFeeTypeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.PlatformFeeTypeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.billing.PlatformFeeTypeSpecifications;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeBasis;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTypeStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class PlatformFeeTypePersistenceAdapter implements PlatformFeeTypePortOut {

    private final PlatformFeeTypeRepository repository;
    private final PlatformFeePersistenceMapper mapper;

    public PlatformFeeTypePersistenceAdapter(
            PlatformFeeTypeRepository repository,
            PlatformFeePersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public PlatformFeeType save(PlatformFeeType feeType) {
        PlatformFeeTypeEntity saved = repository.saveAndFlush(mapper.toTypeEntity(feeType));
        return mapper.toTypeDomain(saved);
    }

    @Override
    public Optional<PlatformFeeType> findById(UUID feeTypeId) {
        return repository.findById(feeTypeId).map(mapper::toTypeDomain);
    }

    @Override
    public Optional<PlatformFeeType> findByFeeCode(String feeCode) {
        return repository.findByFeeCode(feeCode).map(mapper::toTypeDomain);
    }

    @Override
    public PlatformFeePageResult findAll(String keyword, PlatformFeeTypeStatus status,
            PlatformFeeTransactionType transactionType, PlatformFeeBasis feeBasis, int page, int size) {
        Page<PlatformFeeTypeEntity> result = repository.findAll(
                PlatformFeeTypeSpecifications.withFilters(keyword, status, transactionType, feeBasis),
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "feeCode")));
        return new PlatformFeePageResult(
                result.getContent().stream().map(mapper::toTypeDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public List<PlatformFeeType> findActiveByTransactionType(PlatformFeeTransactionType transactionType) {
        return repository.findByTransactionTypeAndStatus(transactionType, PlatformFeeTypeStatus.ACTIVE)
                .stream()
                .map(mapper::toTypeDomain)
                .toList();
    }

    @Override
    public void lockById(UUID feeTypeId) {
        repository.findByIdForUpdate(feeTypeId)
                .orElseThrow(() -> new IllegalStateException("Platform fee type disappeared while locking"));
    }

    @Override
    public void lockByTransactionType(PlatformFeeTransactionType transactionType) {
        repository.findAllByTransactionTypeForUpdate(transactionType);
    }

    @Override
    public boolean existsByFeeCode(String feeCode) {
        return repository.existsByFeeCode(feeCode);
    }
}
