package com.ban.vehicle_management.domain.parking.zone.policy;

import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;

public class ZonePolicy {

    public void initialize(Zone zone) {
        requireZone(zone);
        zone.setCode(TextValidationUtils.normalizeCode(zone.getCode(), "code", 50));
        zone.setName(TextValidationUtils.normalizeRequiredText(zone.getName(), "name", 150));
        requireField(zone.getParkingLotId(), "parkingLotId");

        if (zone.getCapacity() == null) {
            zone.setCapacity(0);
        }
        if (zone.getStatus() == null) {
            zone.setStatus(ZoneStatus.ACTIVE);
        }

        validateState(zone);
    }

    public void activate(Zone zone) {
        requireZone(zone);
        zone.setStatus(ZoneStatus.ACTIVE);
        validateState(zone);
    }

    public void markMaintenance(Zone zone) {
        requireZone(zone);
        zone.setStatus(ZoneStatus.MAINTENANCE);
        validateState(zone);
    }

    public void close(Zone zone) {
        requireZone(zone);
        zone.setStatus(ZoneStatus.CLOSED);
        validateState(zone);
    }

    public void validateState(Zone zone) {
        requireZone(zone);
        zone.setCode(TextValidationUtils.normalizeCode(zone.getCode(), "code", 50));
        zone.setName(TextValidationUtils.normalizeRequiredText(zone.getName(), "name", 150));
        requireField(zone.getParkingLotId(), "parkingLotId");
        requireField(zone.getStatus(), "status");

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
}