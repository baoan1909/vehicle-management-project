package com.ban.vehicle_management.application.billing.wallet.port.in;

import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import java.math.BigDecimal;
import java.util.UUID;

public interface WalletPaymentPortIn {

    Payment payInvoice(UUID invoiceId, String idempotencyKey);

    Payment refundPayment(UUID paymentId, BigDecimal amount, String idempotencyKey, String reason);
}
