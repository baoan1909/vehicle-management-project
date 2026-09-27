package com.ban.vehicle_management.infrastructure.config.time;

import com.ban.vehicle_management.shared.time.ApplicationTimeService;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import jakarta.annotation.PostConstruct;
import java.time.Clock;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ApplicationTimeProperties.class)
public class ApplicationTimeConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationTimeConfiguration.class);

    private final ApplicationTimeProperties properties;

    public ApplicationTimeConfiguration(ApplicationTimeProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void configureLegacyDateTimeFacade() {
        DateTimeUtils.configureAppZone(properties.zoneId());
        LOGGER.info("Application timezone resolved to {}", properties.timeZone());
    }

    @Bean
    public ZoneId applicationZoneId() {
        return properties.zoneId();
    }

    @Bean
    public Clock applicationClock(ZoneId applicationZoneId) {
        return Clock.system(applicationZoneId);
    }

    @Bean
    public ApplicationTimeService applicationTimeService(ZoneId applicationZoneId, Clock applicationClock) {
        return new ApplicationTimeService(applicationZoneId, applicationClock);
    }
}
