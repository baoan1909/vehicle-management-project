package com.ban.vehicle_management.application.billing.wallet.port.out;

import com.ban.vehicle_management.domain.billing.wallet.model.Wallet;
import com.ban.vehicle_management.shared.enumeration.billing.WalletOwnerType;
import com.ban.vehicle_management.shared.enumeration.billing.WalletPurpose;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletPortOut {

    Wallet save(Wallet wallet);

    Wallet createIfAbsent(Wallet wallet);

    Optional<Wallet> findById(UUID walletId);

    Optional<Wallet> findByIdForUpdate(UUID walletId);

    Optional<Wallet> findCustomerWallet(UUID customerId, String currency, WalletPurpose purpose);

    Optional<Wallet> findCustomerWalletForUpdate(UUID customerId, String currency, WalletPurpose purpose);

    Optional<Wallet> findOrganizationWallet(UUID organizationId, String currency, WalletPurpose purpose);

    Optional<Wallet> findOrganizationWalletForUpdate(UUID organizationId, String currency, WalletPurpose purpose);

    List<Wallet> findAll(int page, int size);

    boolean existsByOwner(WalletOwnerType ownerType, UUID customerId, UUID organizationId);
}
