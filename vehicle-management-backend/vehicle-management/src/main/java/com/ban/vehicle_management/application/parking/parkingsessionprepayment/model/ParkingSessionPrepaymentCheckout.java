package com.ban.vehicle_management.application.parking.parkingsessionprepayment.model;

import com.ban.vehicle_management.application.billing.payment.model.result.SepayPaymentResult;

public record ParkingSessionPrepaymentCheckout(
        ParkingSessionPrepaymentView prepayment,
        SepayPaymentResult payment
) {
}
