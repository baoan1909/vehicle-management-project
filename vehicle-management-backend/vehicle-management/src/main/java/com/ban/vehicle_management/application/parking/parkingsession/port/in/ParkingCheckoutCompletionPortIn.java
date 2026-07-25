package com.ban.vehicle_management.application.parking.parkingsession.port.in;

import java.util.UUID;

public interface ParkingCheckoutCompletionPortIn {

    void completePaidCheckout(UUID invoiceId);
}
