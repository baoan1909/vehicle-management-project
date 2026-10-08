package com.ban.vehicle_management.infrastructure.persistence.adapter.billing;

import com.ban.vehicle_management.application.billing.wallet.port.out.WalletAdjustmentPortOut;
import com.ban.vehicle_management.domain.billing.wallet.model.WalletAdjustment;
import com.ban.vehicle_management.infrastructure.mapper.billing.WalletPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.WalletAdjustmentRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class WalletAdjustmentPersistenceAdapter implements WalletAdjustmentPortOut {

    private final WalletAdjustmentRepository repository;
    private final WalletPersistenceMapper mapper;

    public WalletAdjustmentPersistenceAdapter(
            WalletAdjustmentRepository repository, WalletPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public WalletAdjustment save(WalletAdjustment adjustment) {
        return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(adjustment)));
    }

    @Override
    public Optional<WalletAdjustment> findById(UUID adjustmentId) {
        return repository.findById(adjustmentId).map(mapper::toDomain);
    }

    @Override
    public Optional<WalletAdjustment> findByIdForUpdate(UUID adjustmentId) {
        return repository.findByIdForUpdate(adjustmentId).map(mapper::toDomain);
    }

    @Override
    public List<WalletAdjustment> findByWallet(UUID walletId) {
        return repository.findByWalletIdOrderByRequestedAtDesc(walletId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
