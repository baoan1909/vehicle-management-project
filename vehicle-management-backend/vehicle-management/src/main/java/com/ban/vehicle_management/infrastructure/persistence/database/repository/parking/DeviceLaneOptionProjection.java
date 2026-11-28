package com.ban.vehicle_management.infrastructure.persistence.database.repository.parking;

import java.util.UUID;

public interface DeviceLaneOptionProjection {
    UUID getLaneId();
    UUID getParkingLotId();
    UUID getGateId();
    String getCode();
    String getName();
    String getDirection();
    String getStatus();
    String getGateName();
    String getZoneName();
}
