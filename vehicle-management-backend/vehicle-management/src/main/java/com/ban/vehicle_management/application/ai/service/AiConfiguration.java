package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.infrastructure.cache.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({AiAssistantProperties.class, EmbeddingProperties.class, KnowledgeIngestionProperties.class, RetrievalProperties.class, AiCacheProperties.class, AiCircuitBreakerProperties.class, AiQualityProperties.class, RedisProperties.class})
public class AiConfiguration {
}
