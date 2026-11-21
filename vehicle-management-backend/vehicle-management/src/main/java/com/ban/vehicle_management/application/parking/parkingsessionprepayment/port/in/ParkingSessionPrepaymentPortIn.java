package com.ban.vehicle_management.application.parking.parkingsessionprepayment.port.in;

import com.ban.vehicle_management.application.parking.parkingsessionprepayment.model.ParkingSessionPrepaymentView;
import java.time.Instant;
import java.util.UUID;

public interface ParkingSessionPrepaymentPortIn {
    ParkingSessionPrepaymentView lookup(String licensePlate, String cardNumber);
    ParkingSessionPrepaymentView create(
            String licensePlate,
            String cardNumber,
            int estimatedMinutes,
            boolean policyAccepted
    );
    ParkingSessionPrepaymentView get(UUID prepaymentId, String licensePlate, String cardNumber);
    UUID authorizePayment(UUID prepaymentId, String licensePlate, String cardNumber);
    void onPaymentConfirmed(UUID invoiceId, Instant paidAt);
}
