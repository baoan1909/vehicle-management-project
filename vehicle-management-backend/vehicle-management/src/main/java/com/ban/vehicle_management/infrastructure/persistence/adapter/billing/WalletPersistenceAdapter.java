package com.ban.vehicle_management.infrastructure.persistence.adapter.billing;

import com.ban.vehicle_management.application.billing.wallet.port.out.WalletPortOut;
import com.ban.vehicle_management.domain.billing.wallet.model.Wallet;
import com.ban.vehicle_management.infrastructure.mapper.billing.WalletPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.WalletRepository;
import com.ban.vehicle_management.shared.enumeration.billing.WalletOwnerType;
import com.ban.vehicle_management.shared.enumeration.billing.WalletPurpose;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class WalletPersistenceAdapter implements WalletPortOut {

    private final WalletRepository walletRepository;
    private final WalletPersistenceMapper mapper;

    public WalletPersistenceAdapter(WalletRepository walletRepository, WalletPersistenceMapper mapper) {
        this.walletRepository = walletRepository;
        this.mapper = mapper;
    }

    @Override
    public Wallet save(Wallet wallet) {
        return mapper.toDomain(walletRepository.saveAndFlush(mapper.toEntity(wallet)));
    }

    @Override
    public Optional<Wallet> findById(UUID walletId) {
        return walletRepository.findById(walletId).map(mapper::toDomain);
    }

    @Override
    public Optional<Wallet> findByIdForUpdate(UUID walletId) {
        return walletRepository.findByIdForUpdate(walletId).map(mapper::toDomain);
    }

    @Override
    public Optional<Wallet> findCustomerWallet(UUID customerId, String currency, WalletPurpose purpose) {
        return walletRepository
                .findByCustomerIdAndCurrencyAndWalletPurpose(customerId, currency, purpose)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Wallet> findCustomerWalletForUpdate(UUID customerId, String currency, WalletPurpose purpose) {
        return walletRepository
                .findCustomerWalletForUpdate(customerId, currency, purpose)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Wallet> findOrganizationWallet(UUID organizationId, String currency, WalletPurpose purpose) {
        return walletRepository
                .findByOrganizationIdAndCurrencyAndWalletPurpose(organizationId, currency, purpose)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Wallet> findOrganizationWalletForUpdate(UUID organizationId, String currency, WalletPurpose purpose) {
        return walletRepository
                .findOrganizationWalletForUpdate(organizationId, currency, purpose)
                .map(mapper::toDomain);
    }

    @Override
    public List<Wallet> findAll(int page, int size) {
        return walletRepository.findAll(PageRequest.of(page, size)).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByOwner(WalletOwnerType ownerType, UUID customerId, UUID organizationId) {
        return switch (ownerType) {
            case CUSTOMER -> walletRepository
                    .findByCustomerIdAndCurrencyAndWalletPurpose(customerId, "VND", WalletPurpose.PERSONAL)
                    .isPresent();
            case ORGANIZATION -> walletRepository
                    .findByOrganizationIdAndCurrencyAndWalletPurpose(
                            organizationId, "VND", WalletPurpose.ORGANIZATION_SETTLEMENT)
                    .isPresent();
            case PLATFORM -> false;
        };
    }
}
