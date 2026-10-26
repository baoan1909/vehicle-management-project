package com.ban.vehicle_management.application.operations.guestsupport.config;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.public-support")
public class GuestSupportProperties {

    private Duration rateWindow = Duration.ofHours(1);
    private int maxRequestsPerIp = 20;
    private int maxRequestsPerContact = 5;
    private int maxRequestsPerSession = 10;
    private int maxAssistantMessagesPerWindow = 20;
    private boolean rateLimitLocalFallbackEnabled = false;
    private int maxCaptchaVerificationsPerWindow = 10;
    private Duration capabilityTtl = Duration.ofDays(7);
    private Duration retention = Duration.ofDays(90);
    private String capabilitySecret = "";
    private boolean captchaEnabled = false;
    private String captchaProvider = "TURNSTILE";
    private String captchaSiteKey = "";
    private String captchaSecretKey = "";
    private String captchaVerifyUrl = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    private Set<String> captchaExpectedHostnames = new LinkedHashSet<>();
    private Duration captchaProofTtl = Duration.ofMinutes(3);
    private boolean captchaRequiredForIntake = false;
    private boolean captchaRequiredForAssistant = false;
}
