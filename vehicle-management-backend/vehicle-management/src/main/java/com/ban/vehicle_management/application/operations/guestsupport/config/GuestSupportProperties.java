package com.ban.vehicle_management.application.operations.guestsupport.config;

import java.time.Duration;
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
    private int maxAssistantRateKeys = 100_000;
    private Duration capabilityTtl = Duration.ofDays(7);
    private Duration retention = Duration.ofDays(90);
    private String capabilitySecret = "";
}
