package com.ban.vehicle_management.domain.parking.parkinglot.policy;

import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.enumeration.parking.AddressInputScheme;
import com.ban.vehicle_management.shared.enumeration.parking.GeocodingStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.math.BigDecimal;

public class ParkingLotPolicy {

    public void initialize(ParkingLot parkingLot) {
        requireParkingLot(parkingLot);
        parkingLot.setCode(TextValidationUtils.normalizeCode(parkingLot.getCode(), "code", 50));
        parkingLot.setName(TextValidationUtils.normalizeRequiredText(parkingLot.getName(), "name", 150));
        normalizeAddress(parkingLot);
        validateCoordinates(parkingLot);

        if (parkingLot.getTotalCapacity() == null) {
            parkingLot.setTotalCapacity(0);
        }
        if (parkingLot.getStatus() == null) {
            parkingLot.setStatus(ParkingLotStatus.SETUP);
        }
        if (parkingLot.getGeocodingStatus() == null) {
            parkingLot.setGeocodingStatus(
                    parkingLot.getLatitude() == null
                            ? GeocodingStatus.NOT_REQUESTED
                            : GeocodingStatus.NEEDS_REVIEW
            );
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
        normalizeAddress(parkingLot);
        validateCoordinates(parkingLot);
        requireField(parkingLot.getStatus(), "status");
        if (parkingLot.getGeocodingStatus() == null) {
            parkingLot.setGeocodingStatus(
                    parkingLot.getLatitude() == null
                            ? GeocodingStatus.NOT_REQUESTED
                            : GeocodingStatus.NEEDS_REVIEW
            );
        }
        requireField(parkingLot.getGeocodingStatus(), "geocodingStatus");

        if (parkingLot.getStatus() == ParkingLotStatus.ACTIVE) {
            if (parkingLot.getAddressDisplay() == null
                    || parkingLot.getCurrentWardCode() == null
                    || parkingLot.getLatitude() == null
                    || parkingLot.getGeocodingStatus() == GeocodingStatus.NEEDS_REVIEW
                    || parkingLot.getGeocodingStatus() == GeocodingStatus.FAILED
                    || parkingLot.getGeocodingStatus() == GeocodingStatus.NOT_REQUESTED) {
                throw new BadRequestException("active parking lot requires a verified address and location");
            }
        }

        Integer totalCapacity = parkingLot.getTotalCapacity() == null ? 0 : parkingLot.getTotalCapacity();
        if (totalCapacity < 0) {
            throw new BadRequestException("totalCapacity must not be negative");
        }
        parkingLot.setTotalCapacity(totalCapacity);
    }

    private void normalizeAddress(ParkingLot parkingLot) {
        String display = TextValidationUtils.normalizeNullableText(
                parkingLot.getAddressDisplay(),
                "addressDisplay",
                500
        );
        parkingLot.setAddressDisplay(display);
        parkingLot.setCurrentWardCode(normalizeCode(parkingLot.getCurrentWardCode()));
        parkingLot.setLegacyWardCode(normalizeCode(parkingLot.getLegacyWardCode()));

        AddressInputScheme scheme = parkingLot.getAddressInputScheme();
        if (scheme == null && display != null) {
            scheme = AddressInputScheme.CURRENT;
            parkingLot.setAddressInputScheme(scheme);
        }
        if (scheme == AddressInputScheme.CURRENT && parkingLot.getLegacyWardCode() != null) {
            throw new BadRequestException("legacyWardCode is only allowed for a legacy address");
        }
        if (scheme == AddressInputScheme.LEGACY && parkingLot.getLegacyWardCode() == null) {
            throw new BadRequestException("legacyWardCode is required for a legacy address");
        }
    }

    private String normalizeCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
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
