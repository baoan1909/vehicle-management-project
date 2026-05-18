package com.ban.vehicle_management.shared.utils;

import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.Locale;
import java.util.regex.Pattern;

public final class TextValidationUtils {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9_-]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]+$");
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[A-Za-z0-9]+$");

    private TextValidationUtils() {
    }

    public static String normalizeRequiredText(String value, String fieldName, int maxLength) {
        String normalizedValue = normalizeNullableText(value, fieldName, maxLength);
        if (normalizedValue == null) {
            throw new BadRequestException(fieldName + " must not be blank");
        }
        return normalizedValue;
    }

    public static String normalizeNullableText(String value, String fieldName, int maxLength) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        if (normalizedValue.isEmpty()) {
            return null;
        }

        validateCommonText(normalizedValue, fieldName, maxLength);
        return normalizedValue;
    }

    public static String normalizeCode(String value, String fieldName, int maxLength) {
        String normalizedValue = normalizeRequiredText(value, fieldName, maxLength).toUpperCase(Locale.ROOT);
        if (!CODE_PATTERN.matcher(normalizedValue).matches()) {
            throw new BadRequestException(fieldName + " must contain only uppercase letters, digits, underscore, or hyphen");
        }
        return normalizedValue;
    }

    public static String normalizePhoneNumber(String value, String fieldName, int maxLength) {
        String normalizedValue = normalizeNullableText(value, fieldName, maxLength);
        if (normalizedValue == null) {
            return null;
        }
        if (!PHONE_PATTERN.matcher(normalizedValue).matches()) {
            throw new BadRequestException(fieldName + " must contain only digits and an optional leading plus sign");
        }
        return normalizedValue;
    }

    public static String normalizeAlphaNumeric(String value, String fieldName, int maxLength) {
        String normalizedValue = normalizeNullableText(value, fieldName, maxLength);
        if (normalizedValue == null) {
            return null;
        }
        if (!ALPHANUMERIC_PATTERN.matcher(normalizedValue).matches()) {
            throw new BadRequestException(fieldName + " must contain only letters and digits");
        }
        return normalizedValue;
    }

    private static void validateCommonText(String value, String fieldName, int maxLength) {
        if (maxLength > 0 && value.length() > maxLength) {
            throw new BadRequestException(fieldName + " must not exceed " + maxLength + " characters");
        }

        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isISOControl(character) || character == '<' || character == '>') {
                throw new BadRequestException(fieldName + " contains unsupported characters");
            }
        }
    }
}
