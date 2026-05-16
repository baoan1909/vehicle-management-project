package com.ban.vehicle_management.domain.parking.parkingevent.policy;

import com.ban.vehicle_management.domain.parking.parkingevent.model.ParkingEvent;
import com.ban.vehicle_management.shared.enumeration.ParkingEventType;
import com.ban.vehicle_management.shared.exception.BadRequestException;

public class ParkingEventPolicy {

    public void initialize(ParkingEvent parkingEvent) {
        validateState(parkingEvent);
    }

    public void validateState(ParkingEvent parkingEvent) {
        requireParkingEvent(parkingEvent);
        requireField(parkingEvent.getParkingSessionId(), "parkingSessionId");
        requireField(parkingEvent.getLaneId(), "laneId");
        requireField(parkingEvent.getEventType(), "eventType");
        requireField(parkingEvent.getEventTime(), "eventTime");

        parkingEvent.setLicensePlateDetected(normalizeNullable(parkingEvent.getLicensePlateDetected()));
        parkingEvent.setImagePath(normalizeNullable(parkingEvent.getImagePath()));
        parkingEvent.setNote(normalizeNullable(parkingEvent.getNote()));

        if (parkingEvent.getEventType() == ParkingEventType.CHECK_IN
                || parkingEvent.getEventType() == ParkingEventType.CHECK_OUT) {
            parkingEvent.setLicensePlateDetected(
                    normalizeRequired(parkingEvent.getLicensePlateDetected(), "licensePlateDetected"));
        }
    }

    private void requireParkingEvent(ParkingEvent parkingEvent) {
        requireField(parkingEvent, "parkingEvent");
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

