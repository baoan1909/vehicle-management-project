package com.ban.vehicle_management.application.billing.payment.usecase;

import static org.mockito.Mockito.inOrder;

import com.ban.vehicle_management.application.billing.outbox.port.out.BillingOutboxPortOut;
import com.ban.vehicle_management.application.billing.wallet.port.in.SettlementPortIn;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentCompletionUseCaseImplTest {

    @Mock
    private SettlementPortIn settlementPortIn;
    @Mock
    private BillingOutboxPortOut billingOutboxPortOut;

    @Test
    void createsAssessmentBeforeEnqueuingDownstreamCompletion() {
        UUID paymentId = UUID.randomUUID();
        PaymentCompletionUseCaseImpl useCase = new PaymentCompletionUseCaseImpl(
                settlementPortIn, billingOutboxPortOut);

        useCase.completePaymentTruth(paymentId);

        InOrder order = inOrder(settlementPortIn, billingOutboxPortOut);
        order.verify(settlementPortIn).prepareAssessmentForCompletedPayment(paymentId);
        order.verify(billingOutboxPortOut).enqueuePaymentCompleted(
                org.mockito.ArgumentMatchers.eq(paymentId),
                org.mockito.ArgumentMatchers.eq(paymentId.toString()),
                org.mockito.ArgumentMatchers.any());
    }
}
