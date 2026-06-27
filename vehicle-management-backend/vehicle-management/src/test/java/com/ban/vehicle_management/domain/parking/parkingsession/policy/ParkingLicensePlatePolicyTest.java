package com.ban.vehicle_management.domain.parking.parkingsession.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;

class ParkingLicensePlatePolicyTest {

    private final ParkingLicensePlatePolicy parkingLicensePlatePolicy = new ParkingLicensePlatePolicy();

    @Test
    void shouldNormalizeRequiredLicensePlate() {
        assertEquals("51A-12345", parkingLicensePlatePolicy.normalizeRequired(" 51A-12345 ", "licensePlate"));
    }

    @Test
    void shouldMatchLicensePlateIgnoringCaseAndWhitespace() {
        assertTrue(parkingLicensePlatePolicy.matches(" 51A-12345 ", "51a-12345"));
    }

    @Test
    void shouldReturnFalseWhenLicensePlateDoesNotMatch() {
        assertFalse(parkingLicensePlatePolicy.matches("51A-12345", "51A-99999"));
    }

    @Test
    void shouldRejectBlankLicensePlate() {
        assertThrows(
                BadRequestException.class,
                () -> parkingLicensePlatePolicy.normalizeRequired(" ", "licensePlate")
        );
    }
}
