package com.ban.vehicle_management.infrastructure.security.guestsupport;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.application.operations.guestsupport.config.GuestSupportProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class CaptchaStartupValidatorTest {

    @Test
    void productionFailsClosedWhenCaptchaIsDisabledOrOptional() {
        GuestSupportProperties disabled = new GuestSupportProperties();
        MockEnvironment production = new MockEnvironment().withProperty("app.redis.environment", "production");
        assertThrows(IllegalStateException.class,
                () -> new CaptchaStartupValidator(disabled, production).afterSingletonsInstantiated());

        GuestSupportProperties unsafeFallback = validProperties();
        unsafeFallback.setRateLimitLocalFallbackEnabled(true);
        assertThrows(IllegalStateException.class,
                () -> new CaptchaStartupValidator(unsafeFallback, production).afterSingletonsInstantiated());

        GuestSupportProperties optional = validProperties();
        optional.setCaptchaRequiredForAssistant(false);
        assertThrows(IllegalStateException.class,
                () -> new CaptchaStartupValidator(optional, production).afterSingletonsInstantiated());
    }

    @Test
    void enabledCaptchaRequiresProviderKeysHttpsUrlAndPositiveTtl() {
        GuestSupportProperties missingKeys = new GuestSupportProperties();
        missingKeys.setCaptchaEnabled(true);
        assertThrows(IllegalStateException.class,
                () -> validator(missingKeys).afterSingletonsInstantiated());

        GuestSupportProperties insecureUrl = validProperties();
        insecureUrl.setCaptchaVerifyUrl("http://captcha.example.test/verify");
        assertThrows(IllegalStateException.class,
                () -> validator(insecureUrl).afterSingletonsInstantiated());

        GuestSupportProperties invalidTtl = validProperties();
        invalidTtl.setCaptchaProofTtl(java.time.Duration.ZERO);
        assertThrows(IllegalStateException.class,
                () -> validator(invalidTtl).afterSingletonsInstantiated());
    }

    @Test
    void localMayExplicitlyDisableCaptchaAndValidProductionConfigurationStarts() {
        assertDoesNotThrow(() -> validator(new GuestSupportProperties()).afterSingletonsInstantiated());

        MockEnvironment production = new MockEnvironment().withProperty("app.redis.environment", "prod");
        assertDoesNotThrow(() -> new CaptchaStartupValidator(validProperties(), production)
                .afterSingletonsInstantiated());
    }

    private CaptchaStartupValidator validator(GuestSupportProperties properties) {
        return new CaptchaStartupValidator(properties, new MockEnvironment());
    }

    private GuestSupportProperties validProperties() {
        GuestSupportProperties properties = new GuestSupportProperties();
        properties.setCaptchaEnabled(true);
        properties.setCaptchaRequiredForIntake(true);
        properties.setCaptchaRequiredForAssistant(true);
        properties.setCaptchaSiteKey("site-key");
        properties.setCaptchaSecretKey("secret-key");
        return properties;
    }
}
