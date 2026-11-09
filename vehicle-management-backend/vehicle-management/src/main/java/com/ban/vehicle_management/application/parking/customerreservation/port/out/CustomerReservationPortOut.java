package com.ban.vehicle_management.application.parking.customerreservation.port.out;

import com.ban.vehicle_management.domain.parking.customerreservation.model.CustomerReservation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerReservationPortOut {
    CustomerReservation save(CustomerReservation reservation);
    Optional<CustomerReservation> findById(UUID id);
    Optional<CustomerReservation> findByIdForUpdate(UUID id);
    Optional<CustomerReservation> findByInvoiceIdForUpdate(UUID invoiceId);
    Optional<CustomerReservation> findBySessionId(UUID sessionId);
    Optional<CustomerReservation> findByQrTokenForUpdate(String token);
    List<CustomerReservation> findHeldByPlateForUpdate(UUID parkingLotId, String licensePlate, Instant now);
    List<CustomerReservation> findHeldByPlate(String licensePlate, Instant now);
    List<CustomerReservation> findByCustomerId(UUID customerId);
    List<CustomerReservation> findExpired(Instant now);
    long countActiveByZone(UUID zoneId, Instant now);
    boolean existsActiveBySpace(UUID parkingSpaceId, Instant now);
    boolean existsActiveByPlateAndZone(UUID zoneId, String licensePlate, Instant now);
}
