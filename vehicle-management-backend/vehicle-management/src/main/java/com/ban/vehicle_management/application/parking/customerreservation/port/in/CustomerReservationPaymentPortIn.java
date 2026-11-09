package com.ban.vehicle_management.application.parking.customerreservation.port.in;

import java.time.Instant;
import java.util.UUID;

public interface CustomerReservationPaymentPortIn {
    void onPaymentConfirmed(UUID invoiceId, Instant paidAt);
}
