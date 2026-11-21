package com.ban.vehicle_management.application.parking.parkingsessionprepayment.usecase;

import com.ban.vehicle_management.application.billing.payment.port.in.SepayPaymentPortIn;
import com.ban.vehicle_management.application.parking.parkingsessionprepayment.model.ParkingSessionPrepaymentCheckout;
import com.ban.vehicle_management.application.parking.parkingsessionprepayment.port.in.ParkingSessionPrepaymentPortIn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuestParkingSessionPrepaymentCheckoutUseCase {
    private final ParkingSessionPrepaymentPortIn prepayments;
    private final SepayPaymentPortIn sepay;

    public GuestParkingSessionPrepaymentCheckoutUseCase(
            ParkingSessionPrepaymentPortIn prepayments,
            SepayPaymentPortIn sepay
    ) {
        this.prepayments = prepayments;
        this.sepay = sepay;
    }

    @Transactional
    public ParkingSessionPrepaymentCheckout create(
            String licensePlate,
            String cardNumber,
            int estimatedMinutes,
            boolean policyAccepted
    ) {
        var prepayment = prepayments.create(
                licensePlate, cardNumber, estimatedMinutes, policyAccepted);
        var payment = sepay.createParkingSessionPrepaymentPayment(
                prepayment.prepaymentId(), licensePlate, cardNumber);
        return new ParkingSessionPrepaymentCheckout(prepayment, payment);
    }
}
