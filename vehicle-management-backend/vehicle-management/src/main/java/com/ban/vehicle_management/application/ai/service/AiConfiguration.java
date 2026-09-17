package com.ban.vehicle_management.application.ai.service;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({AiAssistantProperties.class, EmbeddingProperties.class})
public class AiConfiguration {
}
