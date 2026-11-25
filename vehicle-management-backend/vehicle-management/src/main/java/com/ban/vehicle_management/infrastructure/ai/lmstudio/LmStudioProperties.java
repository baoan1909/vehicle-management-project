package com.ban.vehicle_management.infrastructure.ai.lmstudio;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ai.lm-studio")
public class LmStudioProperties {
    private String baseUrl = "http://127.0.0.1:1234/v1";
    private String apiKey = "";
    private String chatModel = "qwen3.5-4b";
    private String embeddingModel = "text-embedding-qwen3-embedding-0.6b";
    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration generationTimeout = Duration.ofSeconds(30);
    private Duration embeddingTimeout = Duration.ofSeconds(15);
    private int contextTokens = 2048;
    private int maxOutputTokens = 512;
    private int maxInputBytes = 1200;
    private boolean allowQwenMatryoshka768 = true;
}
