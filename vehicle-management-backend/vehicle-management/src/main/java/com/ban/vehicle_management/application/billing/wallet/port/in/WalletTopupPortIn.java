package com.ban.vehicle_management.application.billing.wallet.port.in;

import com.ban.vehicle_management.application.billing.payment.model.command.VnpayCallbackCommand;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayIpnResult;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayReturnResult;
import com.ban.vehicle_management.domain.billing.wallet.model.WalletTopupOrder;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface WalletTopupPortIn {

    WalletTopupOrder createTopup(BigDecimal amount, String idempotencyKey, String clientIp);

    WalletTopupOrder getTopup(UUID topupOrderId);

    List<WalletTopupOrder> listMyTopups(int page, int size);

    VnpayIpnResult processIpn(VnpayCallbackCommand command);

    VnpayReturnResult verifyReturn(VnpayCallbackCommand command);

    int expireOverdueTopups(int batchSize);

    List<WalletTopupOrder> findReconciliationExceptions(int page, int size);
}
