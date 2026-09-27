package com.ban.vehicle_management.domain.common.licenseplate;

import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Canonical license-plate policy owned by Vehicle Management. OCR output is input data only.
 * Identity is the separator-free {@code normalized} value; punctuation is presentation data.
 */
public class LicensePlatePolicy {

    public static final int MAX_NORMALIZED_LENGTH = 20;
    public static final String FORMAT_VERSION = "VN-PLATE-1";

    private static final Pattern SAFE_INPUT = Pattern.compile("^[A-Za-z0-9 .-]+$");
    private static final Pattern NORMALIZED = Pattern.compile("^[A-Z0-9]+$");
    private static final Pattern FOREIGN = Pattern.compile("^(80)(\\d{3})(NG|QT|CV|NN)(\\d{2,3})$");
    private static final Pattern SPECIAL = Pattern.compile("^(\\d{2})(CD|RM|HC)(\\d{5})$");
    private static final Pattern CAR = Pattern.compile("^(\\d{2}[A-Z])(\\d{5})$");
    private static final Pattern MOTORBIKE_LETTER_DIGIT = Pattern.compile("^(\\d{2}[A-Z]\\d)(\\d{5})$");
    private static final Pattern MOTORBIKE_TWO_LETTER = Pattern.compile("^(\\d{2}[A-Z]{2})(\\d{5})$");
    private static final Pattern MOTORBIKE_LEGACY = Pattern.compile("^(\\d{2}[A-Z]\\d)(\\d{4})$");
    private static final Pattern MILITARY = Pattern.compile("^[A-Z]{2}\\d{4,6}$");

    public String normalizeRequired(String input, String fieldName) {
        String normalized = normalizeNullable(input, fieldName);
        if (normalized == null) {
            throw new BadRequestException(fieldName + " must not be blank");
        }
        return normalized;
    }

    public String normalizeNullable(String input, String fieldName) {
        if (input == null || input.isBlank()) {
            return null;
        }

        String trimmed = input.trim();
        if (!isAscii(trimmed) || !SAFE_INPUT.matcher(trimmed).matches()) {
            throw new BadRequestException(fieldName + " contains unsupported characters");
        }

        String normalized = trimmed
                .toUpperCase(Locale.ROOT)
                .replace(" ", "")
                .replace("-", "")
                .replace(".", "");
        if (!NORMALIZED.matcher(normalized).matches()) {
            throw new BadRequestException(fieldName + " must contain only ASCII letters and digits");
        }
        if (normalized.length() > MAX_NORMALIZED_LENGTH) {
            throw new BadRequestException(fieldName + " must not exceed " + MAX_NORMALIZED_LENGTH + " characters");
        }
        return normalized;
    }

    public LicensePlateResolution resolve(String input, String vehicleTypeCode) {
        String normalized = normalizeNullable(input, "licensePlate");
        String type = vehicleTypeCode == null ? null : vehicleTypeCode.trim().toUpperCase(Locale.ROOT);
        if (normalized == null) {
            if ("BICYCLE".equals(type)) {
                return new LicensePlateResolution(null, null, LicensePlateFormat.NO_PLATE, true, false);
            }
            return new LicensePlateResolution(null, null, LicensePlateFormat.UNKNOWN, false, true);
        }

        Matcher foreign = FOREIGN.matcher(normalized);
        if (foreign.matches()) {
            return valid(normalized,
                    foreign.group(1) + "-" + foreign.group(2) + "-" + foreign.group(3) + "-" + foreign.group(4),
                    LicensePlateFormat.VIETNAM_FOREIGN);
        }

        Matcher special = SPECIAL.matcher(normalized);
        if (special.matches()) {
            return valid(normalized,
                    special.group(1) + special.group(2) + "-" + dottedFiveDigits(special.group(3)),
                    LicensePlateFormat.VIETNAM_SPECIAL);
        }

        if ("CAR".equals(type) || "LIGHT_TRUCK".equals(type) || type == null) {
            Matcher car = CAR.matcher(normalized);
            if (car.matches()) {
                return valid(normalized, car.group(1) + "-" + dottedFiveDigits(car.group(2)),
                        LicensePlateFormat.VIETNAM_CAR);
            }
        }

        if ("MOTORBIKE".equals(type) || type == null) {
            Matcher letterDigit = MOTORBIKE_LETTER_DIGIT.matcher(normalized);
            if (letterDigit.matches()) {
                return valid(normalized, letterDigit.group(1) + "-" + dottedFiveDigits(letterDigit.group(2)),
                        LicensePlateFormat.VIETNAM_MOTORBIKE);
            }
            Matcher twoLetter = MOTORBIKE_TWO_LETTER.matcher(normalized);
            if (twoLetter.matches()) {
                return valid(normalized, twoLetter.group(1) + "-" + dottedFiveDigits(twoLetter.group(2)),
                        LicensePlateFormat.VIETNAM_MOTORBIKE);
            }
            Matcher legacy = MOTORBIKE_LEGACY.matcher(normalized);
            if (legacy.matches()) {
                return valid(normalized, legacy.group(1) + "-" + legacy.group(2),
                        LicensePlateFormat.VIETNAM_MOTORBIKE_LEGACY);
            }
        }

        if (MILITARY.matcher(normalized).matches()) {
            return new LicensePlateResolution(normalized, normalized,
                    LicensePlateFormat.MILITARY_UNSUPPORTED, false, true);
        }
        return new LicensePlateResolution(normalized, normalized, LicensePlateFormat.UNKNOWN, false, true);
    }

    public boolean matches(String expected, String actual) {
        return normalizeRequired(expected, "expectedLicensePlate")
                .equals(normalizeRequired(actual, "detectedLicensePlate"));
    }

    private LicensePlateResolution valid(String normalized, String display, LicensePlateFormat format) {
        return new LicensePlateResolution(normalized, display, format, true, false);
    }

    private String dottedFiveDigits(String digits) {
        return digits.substring(0, 3) + "." + digits.substring(3);
    }

    private boolean isAscii(String value) {
        return value.chars().allMatch(character -> character >= 0x20 && character <= 0x7E);
    }
}
