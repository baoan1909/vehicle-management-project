package com.ban.vehicle_management.infrastructure.ai.gemini;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.gemini")
public class GeminiProperties {

    private String apiKey;
    private String baseUrl = "https://generativelanguage.googleapis.com";
    private String projectId;
    private int maxAttempts = 20;
    private java.time.Duration callBudget = java.time.Duration.ofSeconds(12);
    private java.time.Duration generationMinInterval = java.time.Duration.ofSeconds(5);
    private java.time.Duration embeddingMinInterval = java.time.Duration.ofMillis(650);

    public String getProjectId() { return projectId; }
    public void setProjectId(String value) { projectId = value; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int value) { maxAttempts = value; }
    public java.time.Duration getCallBudget() { return callBudget; }
    public void setCallBudget(java.time.Duration value) { callBudget = value; }
    public java.time.Duration getGenerationMinInterval() { return generationMinInterval; }
    public void setGenerationMinInterval(java.time.Duration value) { generationMinInterval = value; }
    public java.time.Duration getEmbeddingMinInterval() { return embeddingMinInterval; }
    public void setEmbeddingMinInterval(java.time.Duration value) { embeddingMinInterval = value; }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
