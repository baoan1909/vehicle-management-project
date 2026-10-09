package com.ban.vehicle_management.application.parking.parkingspace.port.in;

import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.shared.enumeration.parking.ManualOccupancyType;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;
import java.util.List;
import java.util.UUID;

public interface ParkingSpacePortIn {

    ParkingSpace createParkingSpace(ParkingSpace parkingSpace);

    ParkingSpace getParkingSpaceById(UUID parkingSpaceId);

    List<ParkingSpace> getParkingSpaces(UUID zoneId, ParkingSpaceStatus status, String lifecycleStatus);

    ParkingSpace updateParkingSpace(UUID parkingSpaceId, ParkingSpace parkingSpace);

    void deleteParkingSpace(UUID parkingSpaceId);

    List<ParkingSpace> bulkCreateParkingSpaces(List<ParkingSpace> parkingSpaces);

    ParkingSpace occupyParkingSpace(UUID parkingSpaceId, ManualOccupancyType occupancyType, String reason);

    ParkingSpace releaseParkingSpace(UUID parkingSpaceId);

    ParkingSpace reserveParkingSpace(UUID parkingSpaceId);

    ParkingSpace markParkingSpaceMaintenance(UUID parkingSpaceId);

    ParkingSpace setManualStatus(UUID parkingSpaceId, ParkingSpaceStatus targetStatus, ManualOccupancyType occupancyType, String reason);

    ParkingSpace setAutomaticStatus(UUID parkingSpaceId, ParkingSpaceStatus targetStatus, ManualOccupancyType occupancyType);

    ParkingSpace archiveParkingSpace(UUID parkingSpaceId);
}