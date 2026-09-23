package com.ban.vehicle_management.infrastructure.health;

import com.ban.vehicle_management.application.ai.service.RetrievalProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Production fail-fast for accent-insensitive retrieval: when the
 * {@code prod}/{@code production} profile is active or
 * {@code app.ai.retrieval.unaccent-required} is true, startup fails unless the
 * real {@code unaccent} extension backs {@code ai.immutable_unaccent()}.
 */
@Component
public class RetrievalStartupValidator implements ApplicationRunner {

    private final UnaccentHealthIndicator healthIndicator;
    private final RetrievalProperties properties;
    private final Environment environment;

    public RetrievalStartupValidator(
            UnaccentHealthIndicator healthIndicator,
            RetrievalProperties properties,
            Environment environment
    ) {
        this.healthIndicator = healthIndicator;
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean production = environment.acceptsProfiles(Profiles.of("prod", "production"));
        if (production || properties.isUnaccentRequired()) {
            healthIndicator.assertExtensionReady();
        }
    }
}
