package com.ban.vehicle_management.domain.parking.parkinglot.policy;

import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.enumeration.ParkingLotStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;

public class ParkingLotPolicy {

    public void initialize(ParkingLot parkingLot) {
        requireParkingLot(parkingLot);
        parkingLot.setCode(normalizeRequired(parkingLot.getCode(), "code"));
        parkingLot.setName(normalizeRequired(parkingLot.getName(), "name"));
        parkingLot.setAddress(normalizeNullable(parkingLot.getAddress()));
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
        parkingLot.setCode(normalizeRequired(parkingLot.getCode(), "code"));
        parkingLot.setName(normalizeRequired(parkingLot.getName(), "name"));
        parkingLot.setAddress(normalizeNullable(parkingLot.getAddress()));
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

    private String normalizeRequired(String value, String fieldName) {
        String normalizedValue = normalizeNullable(value);
        if (normalizedValue == null) {
            throw new BadRequestException(fieldName + " must not be blank");
        }
        return normalizedValue;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }
}

