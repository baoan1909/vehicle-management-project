package com.ban.vehicle_management.application.billing.payment.port.in;

import com.ban.vehicle_management.application.billing.payment.model.command.SepayWebhookCommand;
import com.ban.vehicle_management.application.billing.payment.model.result.SepayPaymentResult;
import java.util.UUID;

public interface SepayPaymentPortIn {

    SepayPaymentResult createGuestReservationPayment(UUID reservationId, String accessToken);

    SepayPaymentResult createParkingCheckoutPayment(UUID invoiceId);

    SepayPaymentResult createParkingSessionPrepaymentPayment(
            UUID prepaymentId,
            String licensePlate,
            String cardNumber
    );

    byte[] downloadParkingSessionPrepaymentQr(UUID prepaymentId, String licensePlate, String cardNumber);

    void processWebhook(SepayWebhookCommand command);
}
