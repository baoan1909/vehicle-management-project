package com.ban.vehicle_management.domain.parking.parkingsession.policy;

import com.ban.vehicle_management.domain.common.licenseplate.LicensePlatePolicy;
import com.ban.vehicle_management.domain.common.licenseplate.LicensePlateResolution;

public class ParkingLicensePlatePolicy {

    private final LicensePlatePolicy delegate = new LicensePlatePolicy();

    public String normalizeRequired(String licensePlate, String fieldName) {
        return delegate.normalizeRequired(licensePlate, fieldName);
    }

    public String normalizeNullable(String licensePlate, String fieldName) {
        return delegate.normalizeNullable(licensePlate, fieldName);
    }

    public boolean matches(String expectedLicensePlate, String detectedLicensePlate) {
        return delegate.matches(expectedLicensePlate, detectedLicensePlate);
    }

    public LicensePlateResolution resolve(String licensePlate, String vehicleTypeCode) {
        return delegate.resolve(licensePlate, vehicleTypeCode);
    }
}
