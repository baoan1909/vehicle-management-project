package com.ban.vehicle_management.application.parking.customerreservation.port.in;

import com.ban.vehicle_management.application.parking.customerreservation.model.ReservationOption;
import com.ban.vehicle_management.application.parking.customerreservation.model.ReservationView;
import java.util.List;
import java.util.UUID;

public interface CustomerReservationPortIn {
    List<ReservationOption> findOptions(UUID parkingLotId, UUID vehicleTypeId);
    ReservationView create(UUID parkingLotId, UUID zoneId, UUID parkingSpaceId,
            UUID vehicleTypeId, String licensePlate, boolean policyAccepted);
    ReservationView createGuest(UUID parkingLotId, UUID zoneId, UUID parkingSpaceId,
            UUID vehicleTypeId, String licensePlate, boolean policyAccepted);
    ReservationView getGuest(UUID reservationId, String accessToken);
    ReservationView cancelGuest(UUID reservationId, String accessToken);
    UUID authorizeGuestPayment(UUID reservationId, String accessToken);
    List<ReservationView> findMine();
    ReservationView getMine(UUID reservationId);
    ReservationView cancel(UUID reservationId);
}
