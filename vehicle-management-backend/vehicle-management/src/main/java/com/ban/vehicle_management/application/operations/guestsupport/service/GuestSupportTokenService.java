package com.ban.vehicle_management.application.operations.guestsupport.service;

import com.ban.vehicle_management.application.operations.guestsupport.config.GuestSupportProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GuestSupportTokenService {
    private static final Logger log = LoggerFactory.getLogger(GuestSupportTokenService.class);
    private final byte[] capabilitySecret;

    public GuestSupportTokenService(GuestSupportProperties properties) {
        String configured = properties.getCapabilitySecret();
        if (configured == null || configured.isBlank()) {
            capabilitySecret = new byte[32];
            new SecureRandom().nextBytes(capabilitySecret);
            log.warn("PUBLIC_SUPPORT_CAPABILITY_SECRET is missing; guest capability tokens will not survive a restart");
        } else {
            capabilitySecret = configured.getBytes(StandardCharsets.UTF_8);
        }
    }

    public String capabilityToken(String contactHash, String idempotencyHash) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(capabilitySecret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal((contactHash + ":" + idempotencyHash).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("HMAC-SHA-256 is unavailable", exception);
        }
    }

    public String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public boolean matches(String rawToken, String expectedHash) {
        return MessageDigest.isEqual(hash(rawToken).getBytes(StandardCharsets.US_ASCII),
                expectedHash.getBytes(StandardCharsets.US_ASCII));
    }
}
