package com.ban.vehicle_management.domain.parking.zone.policy;

import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.exception.BadRequestException;

public class ZonePolicy {

    public void initialize(Zone zone) {
        requireZone(zone);
        zone.setCode(normalizeRequired(zone.getCode(), "code"));
        zone.setName(normalizeRequired(zone.getName(), "name"));
        requireField(zone.getParkingLotId(), "parkingLotId");
        if (zone.getCapacity() == null) {
            zone.setCapacity(0);
        }
        validateState(zone);
    }

    public void validateState(Zone zone) {
        requireZone(zone);
        zone.setCode(normalizeRequired(zone.getCode(), "code"));
        zone.setName(normalizeRequired(zone.getName(), "name"));
        requireField(zone.getParkingLotId(), "parkingLotId");

        Integer capacity = zone.getCapacity() == null ? 0 : zone.getCapacity();
        if (capacity < 0) {
            throw new BadRequestException("capacity must not be negative");
        }
        zone.setCapacity(capacity);
    }

    private void requireZone(Zone zone) {
        requireField(zone, "zone");
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}

