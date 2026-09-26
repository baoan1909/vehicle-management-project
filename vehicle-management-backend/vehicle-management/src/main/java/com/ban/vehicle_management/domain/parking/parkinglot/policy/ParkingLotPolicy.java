package com.ban.vehicle_management.domain.parking.parkinglot.policy;

import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.math.BigDecimal;

public class ParkingLotPolicy {

    public void initialize(ParkingLot parkingLot) {
        requireParkingLot(parkingLot);
        parkingLot.setCode(TextValidationUtils.normalizeCode(parkingLot.getCode(), "code", 50));
        parkingLot.setName(TextValidationUtils.normalizeRequiredText(parkingLot.getName(), "name", 150));
        parkingLot.setAddress(TextValidationUtils.normalizeNullableText(parkingLot.getAddress(), "address", 0));
        validateCoordinates(parkingLot);

        if (parkingLot.getTotalCapacity() == null) {
            parkingLot.setTotalCapacity(0);
        }
        if (parkingLot.getStatus() == null) {
            parkingLot.setStatus(ParkingLotStatus.SETUP);
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
        validateCoordinates(parkingLot);
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

    private void validateCoordinates(ParkingLot parkingLot) {
        if (parkingLot.getLatitude() == null && parkingLot.getLongitude() == null) {
            return;
        }
        if (parkingLot.getLatitude() == null || parkingLot.getLongitude() == null) {
            throw new BadRequestException("latitude and longitude must be provided together");
        }
        if (parkingLot.getLatitude().compareTo(BigDecimal.valueOf(-90)) < 0
                || parkingLot.getLatitude().compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new BadRequestException("latitude must be between -90 and 90");
        }
        if (parkingLot.getLongitude().compareTo(BigDecimal.valueOf(-180)) < 0
                || parkingLot.getLongitude().compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new BadRequestException("longitude must be between -180 and 180");
        }
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }
}
