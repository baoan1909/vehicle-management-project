package com.ban.vehicle_management.infrastructure.security.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.iam.account-deletion")
public class AccountDeletionTimeProperties {

    private Duration gracePeriod = Duration.ofDays(14);
    private Duration processingLease = Duration.ofMinutes(5);
    private Duration schedulerDelay = Duration.ofSeconds(30);
    private Duration schedulerInitialDelay = Duration.ofSeconds(30);
    private Duration retryBaseDelay = Duration.ofMinutes(1);
    private int maxAttempts = 10;
    private int batchSize = 25;

    public Duration getGracePeriod() { return gracePeriod; }
    public void setGracePeriod(Duration gracePeriod) { this.gracePeriod = gracePeriod; }
    public Duration getProcessingLease() { return processingLease; }
    public void setProcessingLease(Duration processingLease) { this.processingLease = processingLease; }
    public Duration getSchedulerDelay() { return schedulerDelay; }
    public void setSchedulerDelay(Duration schedulerDelay) { this.schedulerDelay = schedulerDelay; }
    public Duration getSchedulerInitialDelay() { return schedulerInitialDelay; }
    public void setSchedulerInitialDelay(Duration schedulerInitialDelay) { this.schedulerInitialDelay = schedulerInitialDelay; }
    public Duration getRetryBaseDelay() { return retryBaseDelay; }
    public void setRetryBaseDelay(Duration retryBaseDelay) { this.retryBaseDelay = retryBaseDelay; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
}
