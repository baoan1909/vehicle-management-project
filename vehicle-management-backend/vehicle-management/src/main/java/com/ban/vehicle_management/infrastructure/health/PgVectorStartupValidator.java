package com.ban.vehicle_management.infrastructure.health;

import com.ban.vehicle_management.application.ai.service.EmbeddingProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class PgVectorStartupValidator implements ApplicationRunner {

    private final PgVectorHealthIndicator healthIndicator;
    private final EmbeddingProperties properties;
    private final Environment environment;

    public PgVectorStartupValidator(
            PgVectorHealthIndicator healthIndicator,
            EmbeddingProperties properties,
            Environment environment
    ) {
        this.healthIndicator = healthIndicator;
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean production = environment.acceptsProfiles(Profiles.of("prod", "production"));
        if (production || properties.isPgvectorRequired() || properties.isEnabled()) {
            healthIndicator.assertReady();
        }
    }
}
