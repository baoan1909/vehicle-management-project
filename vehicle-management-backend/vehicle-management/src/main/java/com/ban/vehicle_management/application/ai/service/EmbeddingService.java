package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.port.out.EmbeddingProviderPortOut;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.EmbeddingRequest;
import com.ban.vehicle_management.domain.ai.model.EmbeddingResult;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Single application-level retry boundary for embedding calls. The provider adapter is
 * kept free of retry logic: it maps HTTP failures to retryable/non-retryable results and
 * this service decides whether to back off and retry.
 */
@Service
public class EmbeddingService {

    private final List<EmbeddingProviderPortOut> providerPorts;
    private final EmbeddingRetryPolicy retryPolicy;
    private final int maxAttempts;

    public EmbeddingService(List<EmbeddingProviderPortOut> providerPorts, EmbeddingProperties properties) {
        this.providerPorts = providerPorts;
        this.retryPolicy = new EmbeddingRetryPolicy(
                properties.getRetryInitialDelay(),
                properties.getRetryMaxDelay(),
                properties.isRetryJitterEnabled()
        );
        this.maxAttempts = Math.max(1, properties.getRetryMaxAttempts());
    }

    public EmbeddingResult embed(EmbeddingRequest request, AiModelConfiguration configuration) {
        if (configuration == null) {
            throw new BadRequestException("Cấu hình model embedding không hợp lệ");
        }
        EmbeddingProviderPortOut provider = providerPorts.stream()
                .filter(port -> port.provider() == configuration.getProvider())
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Chưa cấu hình embedding provider cho model này"));

        int attempt = 0;
        while (true) {
            EmbeddingResult result = provider.embed(request, configuration);
            if (result.isSuccess() || !result.isRetryable() || attempt >= maxAttempts - 1) {
                return result;
            }
            Long retryAfterSeconds = result.getFailure() == null ? null : result.getFailure().retryAfterSeconds();
            sleepMilliseconds(retryPolicy.nextDelayMillis(attempt, retryAfterSeconds));
            attempt++;
        }
    }

    private void sleepMilliseconds(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Embedding retry interrupted");
        }
    }
}