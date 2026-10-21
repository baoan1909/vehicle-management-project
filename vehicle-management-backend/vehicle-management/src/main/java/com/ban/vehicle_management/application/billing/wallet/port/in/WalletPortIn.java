package com.ban.vehicle_management.application.billing.wallet.port.in;

import com.ban.vehicle_management.domain.billing.wallet.model.FinancialTransaction;
import com.ban.vehicle_management.domain.billing.wallet.model.Wallet;
import java.util.List;
import java.util.UUID;

public interface WalletPortIn {

    Wallet provisionCustomerWallet(UUID customerId);

    Wallet provisionPartnerWallet(UUID organizationId);

    Wallet getMyWallet();

    Wallet getCurrentPartnerWallet();

    Wallet getWalletById(UUID walletId);

    List<Wallet> listWallets(int page, int size);

    List<FinancialTransaction> getMyTransactions(int page, int size);

    List<FinancialTransaction> getCurrentPartnerTransactions(int page, int size);

    List<FinancialTransaction> getWalletTransactions(UUID walletId, int page, int size);
}
