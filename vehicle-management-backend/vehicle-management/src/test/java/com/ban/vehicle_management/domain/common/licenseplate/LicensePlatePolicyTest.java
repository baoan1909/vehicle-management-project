package com.ban.vehicle_management.domain.common.licenseplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;

class LicensePlatePolicyTest {

    private final LicensePlatePolicy policy = new LicensePlatePolicy();

    @Test
    void shouldNormalizeAndFormatCurrentMotorbikePlate() {
        LicensePlateResolution result = policy.resolve(" 59b1-123.45 ", "MOTORBIKE");

        assertEquals("59B112345", result.normalized());
        assertEquals("59B1-123.45", result.display());
        assertEquals("59B1-123.45", policy.resolve("59B112345", "MOTORBIKE").display());
        assertEquals(LicensePlateFormat.VIETNAM_MOTORBIKE, result.format());
        assertTrue(result.validFormat());
        assertFalse(result.needsReview());
    }

    @Test
    void shouldFormatCarAndLegacyMotorbikePlates() {
        assertEquals("30A-123.45", policy.resolve("30A12345", "CAR").display());
        assertEquals("60K8-2301", policy.resolve("60K82301", "MOTORBIKE").display());
    }

    @Test
    void shouldFormatSpecialAndForeignPlates() {
        assertEquals("29CD-123.45", policy.resolve("29CD12345", "OTHER").display());
        assertEquals("80-441-NG-02", policy.resolve("80441NG02", "CAR").display());
    }

    @Test
    void shouldMarkMilitaryAndUnknownFormatsForReview() {
        assertEquals(LicensePlateFormat.MILITARY_UNSUPPORTED, policy.resolve("KT12345", null).format());
        assertTrue(policy.resolve("123ABC", null).needsReview());
    }

    @Test
    void shouldAllowBicycleWithoutFakePlate() {
        LicensePlateResolution result = policy.resolve(null, "BICYCLE");

        assertNull(result.normalized());
        assertEquals(LicensePlateFormat.NO_PLATE, result.format());
        assertTrue(result.validFormat());
    }

    @Test
    void shouldRejectUnicodeConfusablesAndControlCharacters() {
        assertThrows(BadRequestException.class, () -> policy.normalizeRequired("59B1‑123.45", "licensePlate"));
        assertThrows(BadRequestException.class, () -> policy.normalizeRequired("59B1\t12345", "licensePlate"));
    }

    @Test
    void normalizationShouldBeIdempotent() {
        String once = policy.normalizeRequired("59B1-123.45", "licensePlate");
        assertEquals(once, policy.normalizeRequired(once, "licensePlate"));
    }
}
