package com.ban.vehicle_management.shared.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

public final class IdentifierGenerationUtils {

    private static final String CUSTOMER_PREFIX = "CUS";
    private static final String EMPLOYEE_PREFIX = "EMP";
    private static final int EMPLOYEE_CODE_LENGTH = 12;

    private IdentifierGenerationUtils() {
    }

    public static String generateCustomerCode(UUID customerId) {
        return buildCode(
                requireIdentifier(customerId, "customerId"),
                CUSTOMER_PREFIX,
                IdentifierGenerationUtils::encodeSha256Base64Url
        );
    }

    public static String generateEmployeeCode(UUID employeeId) {
        return buildCode(
                requireIdentifier(employeeId, "employeeId"),
                EMPLOYEE_PREFIX,
                IdentifierGenerationUtils::compactUuidPrefix
        );
    }

    private static UUID requireIdentifier(UUID identifier, String fieldName) {
        if (identifier == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return identifier;
    }

    private static String buildCode(UUID identifier, String prefix, java.util.function.Function<UUID, String> encoder) {
        return prefix + "-" + encoder.apply(identifier);
    }

    private static String encodeSha256Base64Url(UUID identifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(identifier.toString().getBytes(StandardCharsets.UTF_8));

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }

    private static String compactUuidPrefix(UUID identifier) {
        return identifier.toString()
                .replace("-", "")
                .substring(0, EMPLOYEE_CODE_LENGTH)
                .toUpperCase();
    }
}
