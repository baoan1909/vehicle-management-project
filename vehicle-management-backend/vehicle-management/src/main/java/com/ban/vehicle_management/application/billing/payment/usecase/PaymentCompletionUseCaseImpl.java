package com.ban.vehicle_management.application.billing.payment.usecase;

import com.ban.vehicle_management.application.billing.outbox.port.out.BillingOutboxPortOut;
import com.ban.vehicle_management.application.billing.payment.port.in.PaymentCompletionPortIn;
import com.ban.vehicle_management.application.billing.wallet.port.in.SettlementPortIn;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentCompletionUseCaseImpl implements PaymentCompletionPortIn {

    private final SettlementPortIn settlementPortIn;
    private final BillingOutboxPortOut billingOutboxPortOut;

    public PaymentCompletionUseCaseImpl(
            SettlementPortIn settlementPortIn,
            BillingOutboxPortOut billingOutboxPortOut) {
        this.settlementPortIn = settlementPortIn;
        this.billingOutboxPortOut = billingOutboxPortOut;
    }

    @Override
    @Transactional
    public void completePaymentTruth(UUID paymentId) {
        settlementPortIn.prepareAssessmentForCompletedPayment(paymentId);
        billingOutboxPortOut.enqueuePaymentCompleted(paymentId, paymentId.toString(), Instant.now());
    }
}
