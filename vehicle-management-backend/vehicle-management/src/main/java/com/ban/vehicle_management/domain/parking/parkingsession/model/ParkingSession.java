package com.ban.vehicle_management.domain.parking.parkingsession.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSession extends AuditableDomainModel {

    private UUID parkingSessionId;
    private UUID cardId;
    private UUID customerId;
    private UUID customerVehicleId;
    private UUID vehicleTypeId;
    private UUID parkingSpaceId;
    private String licensePlateIn;
    private String licensePlateOut;
    private Instant checkInTime;
    private Instant checkOutTime;
    private ParkingSessionStatus status;
    private BigDecimal totalPrice;
    private UUID priceRuleId;
}

