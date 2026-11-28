package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.shared.enumeration.ai.AiDataMode;
import jakarta.validation.constraints.AssertTrue;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.ai")
public class AiAssistantProperties {

    private java.util.Set<String> publicContactValues = java.util.Set.of();
    public java.util.Set<String> getPublicContactValues() { return publicContactValues; }
    public void setPublicContactValues(java.util.Set<String> values) {
        publicContactValues = values == null ? java.util.Set.of() : java.util.Set.copyOf(values);
    }

    private boolean assistantEnabled;
    private int recentMessageLimit = 8;
    @jakarta.validation.constraints.Min(1)
    @jakarta.validation.constraints.Max(4)
    private int assistantJobConcurrency = 2;
    private int maxAttempts = 3;
    private boolean allowUnpaidSupportData;
    private Duration retryInitialDelay = Duration.ofSeconds(20);
    private Duration requestTimeout = Duration.ofSeconds(20);
    private Duration generationTimeout = Duration.ofSeconds(20);
    private Duration lockSafetyMargin = Duration.ofSeconds(90);
    private AiDataMode dataMode = AiDataMode.UNPAID;
    private String promptVersion = "support-assistant-v3";

    public boolean isAssistantEnabled() {
        return assistantEnabled;
    }

    public void setAssistantEnabled(boolean assistantEnabled) {
        this.assistantEnabled = assistantEnabled;
    }

    public int getAssistantJobConcurrency() { return assistantJobConcurrency; }

    public void setAssistantJobConcurrency(int value) { assistantJobConcurrency = value; }

    public int getRecentMessageLimit() {
        return recentMessageLimit;
    }

    public void setRecentMessageLimit(int recentMessageLimit) {
        this.recentMessageLimit = recentMessageLimit;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public boolean isAllowUnpaidSupportData() {
        return allowUnpaidSupportData;
    }

    public void setAllowUnpaidSupportData(boolean allowUnpaidSupportData) {
        this.allowUnpaidSupportData = allowUnpaidSupportData;
    }

    public boolean isProviderCallAllowed() {
        return assistantEnabled && (dataMode != AiDataMode.UNPAID || allowUnpaidSupportData);
    }

    public Duration getRetryInitialDelay() {
        return retryInitialDelay;
    }

    public void setRetryInitialDelay(Duration retryInitialDelay) {
        this.retryInitialDelay = retryInitialDelay;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public Duration getGenerationTimeout() {
        return generationTimeout.compareTo(requestTimeout) < 0 ? generationTimeout : requestTimeout;
    }

    public void setGenerationTimeout(Duration generationTimeout) {
        this.generationTimeout = generationTimeout;
    }

    public Duration getLockSafetyMargin() {
        return lockSafetyMargin;
    }

    public void setLockSafetyMargin(Duration lockSafetyMargin) {
        this.lockSafetyMargin = lockSafetyMargin;
    }

    public AiDataMode getDataMode() {
        return dataMode;
    }

    public void setDataMode(AiDataMode dataMode) {
        this.dataMode = dataMode;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    @AssertTrue(message = "AI assistant durations must be greater than zero")
    public boolean areDurationsValid() {
        return isPositive(retryInitialDelay) && isPositive(requestTimeout)
                && isPositive(generationTimeout) && isPositive(lockSafetyMargin);
    }

    private boolean isPositive(Duration value) {
        return value != null && !value.isZero() && !value.isNegative();
    }
}
