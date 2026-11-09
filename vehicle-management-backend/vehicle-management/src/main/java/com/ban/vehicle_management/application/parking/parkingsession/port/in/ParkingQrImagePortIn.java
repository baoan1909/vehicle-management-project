package com.ban.vehicle_management.application.parking.parkingsession.port.in;

import java.util.UUID;

public interface ParkingQrImagePortIn {
    String decodeForOperation(byte[] imageBytes, UUID parkingLotId);
}
