package com.ban.vehicle_management.domain.parking.parkingspace.policy;

import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.shared.enumeration.ParkingSpaceStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;

public class ParkingSpacePolicy {

    public void initialize(ParkingSpace parkingSpace) {
        requireParkingSpace(parkingSpace);
        parkingSpace.setCode(TextValidationUtils.normalizeCode(parkingSpace.getCode(), "code", 50));
        requireField(parkingSpace.getZoneId(), "zoneId");
        if (parkingSpace.getStatus() == null) {
            parkingSpace.setStatus(ParkingSpaceStatus.AVAILABLE);
        }
        validateState(parkingSpace);
    }

    public void occupy(ParkingSpace parkingSpace) {
        requireStatus(parkingSpace, ParkingSpaceStatus.AVAILABLE, ParkingSpaceStatus.RESERVED);
        parkingSpace.setStatus(ParkingSpaceStatus.OCCUPIED);
        validateState(parkingSpace);
    }

    public void release(ParkingSpace parkingSpace) {
        requireStatus(parkingSpace, ParkingSpaceStatus.OCCUPIED, ParkingSpaceStatus.RESERVED);
        parkingSpace.setStatus(ParkingSpaceStatus.AVAILABLE);
        validateState(parkingSpace);
    }

    public void reserve(ParkingSpace parkingSpace) {
        requireStatus(parkingSpace, ParkingSpaceStatus.AVAILABLE);
        parkingSpace.setStatus(ParkingSpaceStatus.RESERVED);
        validateState(parkingSpace);
    }

    public void markMaintenance(ParkingSpace parkingSpace) {
        requireParkingSpace(parkingSpace);
        if (parkingSpace.getStatus() == ParkingSpaceStatus.OCCUPIED) {
            throw new BadRequestException("Occupied parkingSpace must not be moved to maintenance");
        }
        parkingSpace.setStatus(ParkingSpaceStatus.MAINTENANCE);
        validateState(parkingSpace);
    }

    public void validateState(ParkingSpace parkingSpace) {
        requireParkingSpace(parkingSpace);
        parkingSpace.setCode(TextValidationUtils.normalizeCode(parkingSpace.getCode(), "code", 50));
        requireField(parkingSpace.getZoneId(), "zoneId");
        requireField(parkingSpace.getStatus(), "status");
    }

    private void requireStatus(ParkingSpace parkingSpace, ParkingSpaceStatus... expectedStatuses) {
        requireParkingSpace(parkingSpace);
        for (ParkingSpaceStatus expectedStatus : expectedStatuses) {
            if (parkingSpace.getStatus() == expectedStatus) {
                return;
            }
        }
        throw new BadRequestException("ParkingSpace is not in a valid status for this action");
    }

    private void requireParkingSpace(ParkingSpace parkingSpace) {
        requireField(parkingSpace, "parkingSpace");
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }

}

