package com.ban.vehicle_management.application.parking.parkingsessionprepayment.port.out;

import com.ban.vehicle_management.domain.parking.parkingsessionprepayment.model.ParkingSessionPrepayment;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionPrepaymentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParkingSessionPrepaymentPortOut {
    ParkingSessionPrepayment save(ParkingSessionPrepayment value);
    Optional<ParkingSessionPrepayment> findById(UUID id);
    Optional<ParkingSessionPrepayment> findByIdForUpdate(UUID id);
    Optional<ParkingSessionPrepayment> findByInvoiceIdForUpdate(UUID invoiceId);
    List<ParkingSessionPrepayment> findBySessionAndStatus(
            UUID parkingSessionId,
            ParkingSessionPrepaymentStatus status
    );
}
