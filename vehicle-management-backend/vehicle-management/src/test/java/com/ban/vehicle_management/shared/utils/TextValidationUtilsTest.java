package com.ban.vehicle_management.shared.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;

class TextValidationUtilsTest {

    @Test
    void shouldNormalizeUppercaseCode() {
        String normalizedCode = TextValidationUtils.normalizeCode(" car_type-01 ", "code", 50);

        assertEquals("CAR_TYPE-01", normalizedCode);
    }

    @Test
    void shouldRejectUnsupportedCharactersInText() {
        assertThrows(BadRequestException.class, () ->
                TextValidationUtils.normalizeNullableText("hello<world", "name", 100));
    }

    @Test
    void shouldRejectTooLongText() {
        assertThrows(BadRequestException.class, () ->
                TextValidationUtils.normalizeRequiredText("123456", "code", 5));
    }

    @Test
    void shouldNormalizePhoneNumber() {
        String phoneNumber = TextValidationUtils.normalizePhoneNumber(" +84901234567 ", "phoneNumber", 20);

        assertEquals("+84901234567", phoneNumber);
    }

    @Test
    void shouldRejectInvalidPhoneNumber() {
        assertThrows(BadRequestException.class, () ->
                TextValidationUtils.normalizePhoneNumber("0901-234-567", "phoneNumber", 20));
    }

    @Test
    void shouldNormalizeBlankOptionalTextToNull() {
        assertNull(TextValidationUtils.normalizeNullableText("   ", "description", 255));
    }
}
