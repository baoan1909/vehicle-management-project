package com.ban.vehicle_management.infrastructure.config;

import com.ban.vehicle_management.infrastructure.config.time.AppInstantDeserializer;
import com.ban.vehicle_management.infrastructure.config.time.AppInstantHttpSerializer;
import com.ban.vehicle_management.infrastructure.config.time.AppInstantSerializer;
import com.ban.vehicle_management.infrastructure.config.time.ApplicationTimeProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.time.Instant;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper(ApplicationTimeProperties timeProperties) {
        SimpleModule applicationTimeModule = new SimpleModule("application-time");
        applicationTimeModule.addSerializer(Instant.class, new AppInstantSerializer(timeProperties.zoneId()));
        applicationTimeModule.addDeserializer(Instant.class, new AppInstantDeserializer());

        return JsonMapper.builder()
                .findAndAddModules()
                .addModule(applicationTimeModule)
                .build();
    }

    /**
     * Spring Boot 4 uses Jackson 3 for Spring MVC while MinIO still brings in
     * Jackson 2 for internal adapters. Configure the HTTP mapper explicitly so
     * API responses use the application timezone as well.
     */
    @Bean
    public JsonMapperBuilderCustomizer applicationTimeHttpJsonCustomizer(
            ApplicationTimeProperties timeProperties
    ) {
        return builder -> {
            tools.jackson.databind.module.SimpleModule applicationTimeModule =
                    new tools.jackson.databind.module.SimpleModule("application-time-http");
            applicationTimeModule.addSerializer(
                    Instant.class,
                    new AppInstantHttpSerializer(timeProperties.zoneId())
            );
            builder.addModule(applicationTimeModule);
        };
    }
}
