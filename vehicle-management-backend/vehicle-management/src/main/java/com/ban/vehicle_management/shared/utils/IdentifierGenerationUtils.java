package com.ban.vehicle_management.shared.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

public final class IdentifierGenerationUtils {

    private IdentifierGenerationUtils() {
    }

    public static String generateCustomerCode(UUID customerId) {
        if (customerId == null) {
            throw new IllegalArgumentException("customerId must not be null");
        }

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(customerId.toString().getBytes(StandardCharsets.UTF_8));

            String encodedDigest = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(digest);

            return "CUS-" + encodedDigest;
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }
}
