package com.ban.vehicle_management.domain.parking.parkingsession.policy;

import com.ban.vehicle_management.shared.utils.TextValidationUtils;

public class ParkingLicensePlatePolicy {

    public String normalizeRequired(String licensePlate, String fieldName) {
        return TextValidationUtils.normalizeRequiredText(licensePlate, fieldName, 20);
    }

    public String normalizeNullable(String licensePlate, String fieldName) {
        return TextValidationUtils.normalizeNullableText(licensePlate, fieldName, 20);
    }

    public boolean matches(String expectedLicensePlate, String detectedLicensePlate) {
        String normalizedExpected = normalizeRequired(expectedLicensePlate, "expectedLicensePlate");
        String normalizedDetected = normalizeRequired(detectedLicensePlate, "detectedLicensePlate");
        return normalizedExpected.equalsIgnoreCase(normalizedDetected);
    }
}
