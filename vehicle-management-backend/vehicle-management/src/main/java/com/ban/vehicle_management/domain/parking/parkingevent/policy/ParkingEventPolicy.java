package com.ban.vehicle_management.domain.parking.parkingevent.policy;

import com.ban.vehicle_management.domain.parking.parkingevent.model.ParkingEvent;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingEventType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;

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

        parkingEvent.setLicensePlateDetected(TextValidationUtils.normalizeNullableText(parkingEvent.getLicensePlateDetected(), "licensePlateDetected", 20));
        parkingEvent.setLicensePlateImagePath(TextValidationUtils.normalizeNullableText(
                parkingEvent.getLicensePlateImagePath(),
                "licensePlateImagePath",
                255
        ));
        parkingEvent.setPersonImagePath(TextValidationUtils.normalizeNullableText(
                parkingEvent.getPersonImagePath(),
                "personImagePath",
                255
        ));
        parkingEvent.setNote(TextValidationUtils.normalizeNullableText(parkingEvent.getNote(), "note", 0));

        if (parkingEvent.getEventType() == ParkingEventType.CHECK_IN
                || parkingEvent.getEventType() == ParkingEventType.CHECK_OUT) {
            parkingEvent.setLicensePlateDetected(
                    TextValidationUtils.normalizeRequiredText(parkingEvent.getLicensePlateDetected(), "licensePlateDetected", 20));
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

}

