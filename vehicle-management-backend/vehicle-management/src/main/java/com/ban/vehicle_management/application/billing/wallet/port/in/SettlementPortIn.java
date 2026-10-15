package com.ban.vehicle_management.application.billing.wallet.port.in;

import com.ban.vehicle_management.domain.billing.wallet.model.PayoutRequest;
import com.ban.vehicle_management.domain.billing.wallet.model.RevenueAllocation;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SettlementPortIn {

    RevenueAllocation allocateForWalletPayment(UUID paymentId, UUID voucherId);

    int releaseDueSettlements(int batchSize);

    PayoutRequest requestPayout(UUID bankAccountId, BigDecimal amount, String idempotencyKey);

    PayoutRequest approvePayout(UUID payoutRequestId);

    PayoutRequest rejectPayout(UUID payoutRequestId, String reason);

    List<RevenueAllocation> listMyAllocations(int page, int size);

    List<PayoutRequest> listMyPayouts(int page, int size);

    Map<String, BigDecimal> financialOverview();
}
