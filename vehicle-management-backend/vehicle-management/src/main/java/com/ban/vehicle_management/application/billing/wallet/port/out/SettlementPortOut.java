package com.ban.vehicle_management.application.billing.wallet.port.out;

import com.ban.vehicle_management.domain.billing.wallet.model.PartnerBankAccount;
import com.ban.vehicle_management.domain.billing.wallet.model.PayoutRequest;
import com.ban.vehicle_management.domain.billing.wallet.model.RevenueAllocation;
import com.ban.vehicle_management.domain.billing.wallet.model.VoucherFinancialTerm;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SettlementPortOut {

    RevenueAllocation saveAllocation(RevenueAllocation allocation);

    Optional<RevenueAllocation> findAllocationByPayment(UUID paymentId);

    Optional<RevenueAllocation> findAllocationByIdForUpdate(UUID allocationId);

    List<UUID> claimDueAllocations(int batchSize, Instant now);

    Optional<RevenueAllocation> findAllocationById(UUID allocationId);

    List<RevenueAllocation> findByOrganization(UUID organizationId, int page, int size);

    Optional<VoucherFinancialTerm> findActiveTerm(UUID voucherId, Instant at);

    VoucherFinancialTerm saveTerm(VoucherFinancialTerm term);

    PartnerBankAccount saveBankAccount(PartnerBankAccount account);

    Optional<PartnerBankAccount> findBankAccount(UUID bankAccountId);

    List<PartnerBankAccount> findBankAccountsByOrg(UUID organizationId);

    PayoutRequest savePayout(PayoutRequest payout);

    Optional<PayoutRequest> findPayoutById(UUID payoutId);

    Optional<PayoutRequest> findPayoutByIdForUpdate(UUID payoutId);

    Optional<PayoutRequest> findPayoutByIdempotency(String key);

    List<PayoutRequest> findPayoutsByOrg(UUID organizationId, int page, int size);

    java.util.Map<String, java.math.BigDecimal> sumLedgerByAccount();
}
