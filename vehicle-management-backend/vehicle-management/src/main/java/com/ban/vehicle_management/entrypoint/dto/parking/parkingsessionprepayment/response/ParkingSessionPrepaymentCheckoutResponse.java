package com.ban.vehicle_management.entrypoint.dto.parking.parkingsessionprepayment.response;

import com.ban.vehicle_management.entrypoint.dto.billing.payment.response.SepayPaymentResponse;

public record ParkingSessionPrepaymentCheckoutResponse(
        ParkingSessionPrepaymentResponse prepayment,
        SepayPaymentResponse payment
) {
}
