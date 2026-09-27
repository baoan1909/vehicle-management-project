package com.ban.vehicle_management.domain.parking.parkingevent.policy;

import com.ban.vehicle_management.domain.common.licenseplate.LicensePlatePolicy;
import com.ban.vehicle_management.domain.parking.parkingevent.model.ParkingEvent;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingEventType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;

public class ParkingEventPolicy {

    private final LicensePlatePolicy licensePlatePolicy = new LicensePlatePolicy();

    public void initialize(ParkingEvent parkingEvent) {
        validateState(parkingEvent);
    }

    public void validateState(ParkingEvent parkingEvent) {
        requireParkingEvent(parkingEvent);
        requireField(parkingEvent.getParkingSessionId(), "parkingSessionId");
        requireField(parkingEvent.getLaneId(), "laneId");
        requireField(parkingEvent.getEventType(), "eventType");
        requireField(parkingEvent.getEventTime(), "eventTime");

        parkingEvent.setLicensePlateDetected(licensePlatePolicy.normalizeNullable(parkingEvent.getLicensePlateDetected(), "licensePlateDetected"));
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
                || parkingEvent.getEventType() == ParkingEventType.CHECK_OUT_PENDING
                || parkingEvent.getEventType() == ParkingEventType.CHECK_OUT) {
            parkingEvent.setLicensePlateDetected(
                    licensePlatePolicy.normalizeRequired(parkingEvent.getLicensePlateDetected(), "licensePlateDetected"));
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

