package com.ban.vehicle_management.application.accesscontrol.subscription.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionQrTokenService {

    public static final String PAYLOAD_PREFIX = "COPARKING:SUBSCRIPTION:1:";
    private static final int TOKEN_BYTES = 32;
    private static final Pattern TOKEN_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{43}$");

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateToken() {
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    public String toPayload(String token) {
        if (!isValidToken(token)) {
            throw new IllegalArgumentException("Invalid subscription QR token");
        }
        return PAYLOAD_PREFIX + token;
    }

    public Optional<String> extractToken(String payload) {
        if (payload == null || !payload.startsWith(PAYLOAD_PREFIX)) {
            return Optional.empty();
        }
        String token = payload.substring(PAYLOAD_PREFIX.length());
        return isValidToken(token) ? Optional.of(token) : Optional.empty();
    }

    private boolean isValidToken(String token) {
        return token != null && TOKEN_PATTERN.matcher(token).matches();
    }
}
