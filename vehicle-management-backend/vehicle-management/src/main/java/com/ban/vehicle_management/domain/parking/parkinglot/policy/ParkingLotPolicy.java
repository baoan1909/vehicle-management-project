package com.ban.vehicle_management.domain.parking.parkinglot.policy;

import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;

public class ParkingLotPolicy {

    public void initialize(ParkingLot parkingLot) {
        requireParkingLot(parkingLot);
        parkingLot.setCode(TextValidationUtils.normalizeCode(parkingLot.getCode(), "code", 50));
        parkingLot.setName(TextValidationUtils.normalizeRequiredText(parkingLot.getName(), "name", 150));
        parkingLot.setAddress(TextValidationUtils.normalizeNullableText(parkingLot.getAddress(), "address", 0));
        if (parkingLot.getTotalCapacity() == null) {
            parkingLot.setTotalCapacity(0);
        }
        if (parkingLot.getStatus() == null) {
            parkingLot.setStatus(ParkingLotStatus.ACTIVE);
        }
        validateState(parkingLot);
    }

    public void activate(ParkingLot parkingLot) {
        requireParkingLot(parkingLot);
        parkingLot.setStatus(ParkingLotStatus.ACTIVE);
        validateState(parkingLot);
    }

    public void markMaintenance(ParkingLot parkingLot) {
        requireParkingLot(parkingLot);
        parkingLot.setStatus(ParkingLotStatus.MAINTENANCE);
        validateState(parkingLot);
    }

    public void close(ParkingLot parkingLot) {
        requireParkingLot(parkingLot);
        parkingLot.setStatus(ParkingLotStatus.CLOSED);
        validateState(parkingLot);
    }

    public void validateState(ParkingLot parkingLot) {
        requireParkingLot(parkingLot);
        parkingLot.setCode(TextValidationUtils.normalizeCode(parkingLot.getCode(), "code", 50));
        parkingLot.setName(TextValidationUtils.normalizeRequiredText(parkingLot.getName(), "name", 150));
        parkingLot.setAddress(TextValidationUtils.normalizeNullableText(parkingLot.getAddress(), "address", 0));
        requireField(parkingLot.getStatus(), "status");

        Integer totalCapacity = parkingLot.getTotalCapacity() == null ? 0 : parkingLot.getTotalCapacity();
        if (totalCapacity < 0) {
            throw new BadRequestException("totalCapacity must not be negative");
        }
        parkingLot.setTotalCapacity(totalCapacity);
    }

    private void requireParkingLot(ParkingLot parkingLot) {
        requireField(parkingLot, "parkingLot");
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }

}

