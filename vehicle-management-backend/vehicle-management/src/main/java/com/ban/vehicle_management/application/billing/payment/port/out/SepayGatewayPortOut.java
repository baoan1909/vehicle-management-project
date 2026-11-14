package com.ban.vehicle_management.application.billing.payment.port.out;

import com.ban.vehicle_management.application.billing.payment.model.command.SepayWebhookCommand;
import com.ban.vehicle_management.application.billing.payment.model.result.SepayPaymentInstruction;
import com.ban.vehicle_management.application.billing.payment.model.result.SepayWebhookData;
import java.math.BigDecimal;
import java.time.Instant;

public interface SepayGatewayPortOut {

    SepayPaymentInstruction createPaymentInstruction(
            String transactionRef, BigDecimal amount, Instant createdAt);

    SepayWebhookData verifyWebhook(SepayWebhookCommand command);

    String transactionPrefix();

    int monthlyTransactionLimit();
}
