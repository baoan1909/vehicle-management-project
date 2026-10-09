package com.ban.vehicle_management.domain.parking.parkinglevel.policy;

import com.ban.vehicle_management.domain.parking.parkinglevel.model.ParkingLevel;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLevelStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;

public class ParkingLevelPolicy {

    public void initialize(ParkingLevel parkingLevel) {
        requireParkingLevel(parkingLevel);
        parkingLevel.setCode(TextValidationUtils.normalizeCode(parkingLevel.getCode(), "code", 50));
        parkingLevel.setName(TextValidationUtils.normalizeRequiredText(parkingLevel.getName(), "name", 150));
        requireField(parkingLevel.getParkingLotId(), "parkingLotId");

        if (parkingLevel.getDisplayOrder() == null) {
            parkingLevel.setDisplayOrder(0);
        }
        if (parkingLevel.getCanvasWidth() == null) {
            parkingLevel.setCanvasWidth(java.math.BigDecimal.valueOf(10000));
        }
        if (parkingLevel.getCanvasHeight() == null) {
            parkingLevel.setCanvasHeight(java.math.BigDecimal.valueOf(10000));
        }
        if (parkingLevel.getStatus() == null) {
            parkingLevel.setStatus(ParkingLevelStatus.ACTIVE);
        }

        validateState(parkingLevel);
    }

    public void activate(ParkingLevel parkingLevel) {
        requireParkingLevel(parkingLevel);
        parkingLevel.setStatus(ParkingLevelStatus.ACTIVE);
        validateState(parkingLevel);
    }

    public void markMaintenance(ParkingLevel parkingLevel) {
        requireParkingLevel(parkingLevel);
        parkingLevel.setStatus(ParkingLevelStatus.MAINTENANCE);
        validateState(parkingLevel);
    }

    public void close(ParkingLevel parkingLevel) {
        requireParkingLevel(parkingLevel);
        parkingLevel.setStatus(ParkingLevelStatus.CLOSED);
        validateState(parkingLevel);
    }

    public void validateState(ParkingLevel parkingLevel) {
        requireParkingLevel(parkingLevel);
        parkingLevel.setCode(TextValidationUtils.normalizeCode(parkingLevel.getCode(), "code", 50));
        parkingLevel.setName(TextValidationUtils.normalizeRequiredText(parkingLevel.getName(), "name", 150));
        requireField(parkingLevel.getParkingLotId(), "parkingLotId");
        requireField(parkingLevel.getStatus(), "status");

        if (parkingLevel.getDisplayOrder() == null) {
            parkingLevel.setDisplayOrder(0);
        }

        if (parkingLevel.getCanvasWidth() == null || parkingLevel.getCanvasWidth().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("canvasWidth must be positive");
        }
        if (parkingLevel.getCanvasHeight() == null || parkingLevel.getCanvasHeight().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("canvasHeight must be positive");
        }
        if (parkingLevel.getFloorHeight() != null && parkingLevel.getFloorHeight().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("floorHeight must be positive");
        }
    }

    private void requireParkingLevel(ParkingLevel parkingLevel) {
        requireField(parkingLevel, "parkingLevel");
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }
}