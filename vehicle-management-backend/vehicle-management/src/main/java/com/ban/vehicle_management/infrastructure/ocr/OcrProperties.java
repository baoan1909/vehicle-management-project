package com.ban.vehicle_management.infrastructure.ocr;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "app.ocr")
public class OcrProperties {
    private boolean enabled = true;
    private String baseUrl = "http://localhost:8010";
    private String internalToken = "dev-ocr-internal-token";
    private int connectTimeoutMs = 1500;
    private int readTimeoutMs = 5000;
    private double confidenceThreshold = 0.70;
}
