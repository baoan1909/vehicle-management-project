package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.shared.enumeration.ai.AiDataMode;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public class AiAssistantProperties {

    private boolean assistantEnabled;
    private int recentMessageLimit = 8;
    private int maxAttempts = 3;
    private boolean allowUnpaidSupportData;
    private Duration retryInitialDelay = Duration.ofSeconds(20);
    private Duration requestTimeout = Duration.ofSeconds(20);
    private AiDataMode dataMode = AiDataMode.UNPAID;
    private String promptVersion = "support-assistant-v1";

    public boolean isAssistantEnabled() {
        return assistantEnabled;
    }

    public void setAssistantEnabled(boolean assistantEnabled) {
        this.assistantEnabled = assistantEnabled;
    }

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
}
