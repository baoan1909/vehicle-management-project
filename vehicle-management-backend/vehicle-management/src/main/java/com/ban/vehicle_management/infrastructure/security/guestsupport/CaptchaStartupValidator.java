package com.ban.vehicle_management.infrastructure.security.guestsupport;

import com.ban.vehicle_management.application.operations.guestsupport.config.GuestSupportProperties;
import java.net.URI;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class CaptchaStartupValidator implements SmartInitializingSingleton {

    private final GuestSupportProperties properties;
    private final Environment environment;

    public CaptchaStartupValidator(GuestSupportProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void afterSingletonsInstantiated() {
        String deploymentEnvironment = environment.getProperty("app.redis.environment", "");
        boolean production = environment.acceptsProfiles(Profiles.of("prod", "production"))
                || "prod".equalsIgnoreCase(deploymentEnvironment)
                || "production".equalsIgnoreCase(deploymentEnvironment);
        if (production && properties.isRateLimitLocalFallbackEnabled()) {
            throw new IllegalStateException(
                    "Public-support local rate-limit fallback is forbidden in production");
        }
        if (production && (!properties.isCaptchaEnabled()
                || !properties.isCaptchaRequiredForIntake()
                || !properties.isCaptchaRequiredForAssistant())) {
            throw new IllegalStateException(
                    "Public-support CAPTCHA must be enabled and required for intake and assistant in production");
        }
        if (!properties.isCaptchaEnabled()) return;
        if (!"TURNSTILE".equalsIgnoreCase(properties.getCaptchaProvider())) {
            throw new IllegalStateException("Unsupported PUBLIC_SUPPORT_CAPTCHA_PROVIDER");
        }
        if (blank(properties.getCaptchaSiteKey()) || blank(properties.getCaptchaSecretKey())) {
            throw new IllegalStateException("CAPTCHA site key and secret key are required when CAPTCHA is enabled");
        }
        URI verifyUri = URI.create(properties.getCaptchaVerifyUrl());
        if (!"https".equalsIgnoreCase(verifyUri.getScheme()) || blank(verifyUri.getHost())) {
            throw new IllegalStateException("CAPTCHA verification URL must use HTTPS");
        }
        if (properties.getCaptchaProofTtl() == null
                || properties.getCaptchaProofTtl().isZero()
                || properties.getCaptchaProofTtl().isNegative()) {
            throw new IllegalStateException("CAPTCHA proof TTL must be positive");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
