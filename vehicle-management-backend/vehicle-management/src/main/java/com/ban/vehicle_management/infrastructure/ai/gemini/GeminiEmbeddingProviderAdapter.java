package com.ban.vehicle_management.infrastructure.ai.gemini;

import com.ban.vehicle_management.application.ai.port.out.EmbeddingProviderPortOut;
import com.ban.vehicle_management.application.ai.service.AiAssistantProperties;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.EmbeddingFailure;
import com.ban.vehicle_management.domain.ai.model.EmbeddingRequest;
import com.ban.vehicle_management.domain.ai.model.EmbeddingResult;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class GeminiEmbeddingProviderAdapter implements EmbeddingProviderPortOut {

    @Autowired
    private com.ban.vehicle_management.infrastructure.ai.runtime.AiProviderHttpClient providerHttpClient;

    private HttpResponse<String> sendProvider(HttpRequest request, Runnable sent) throws java.io.IOException, InterruptedException {
        if (providerHttpClient == null) {
            sent.run();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        }
        return providerHttpClient.send(AiProvider.GEMINI, httpClient, request, sent);
    }

    private final GeminiProperties geminiProperties;
    private final GeminiEmbeddingRequestMapper requestMapper;
    private final GeminiEmbeddingResponseMapper responseMapper;
    private final HttpClient httpClient;
    private final Duration requestTimeout;
    private final GeminiCredentialPool credentialPool;

    public GeminiEmbeddingProviderAdapter(
            GeminiProperties geminiProperties,
            AiAssistantProperties assistantProperties,
            GeminiEmbeddingRequestMapper requestMapper,
            GeminiEmbeddingResponseMapper responseMapper
    ) {
        this(geminiProperties, assistantProperties, requestMapper, responseMapper, null);
    }

    @Autowired
    public GeminiEmbeddingProviderAdapter(GeminiProperties geminiProperties,
            AiAssistantProperties assistantProperties, GeminiEmbeddingRequestMapper requestMapper,
            GeminiEmbeddingResponseMapper responseMapper, GeminiCredentialPool credentialPool) {
        this.geminiProperties = geminiProperties;
        this.requestMapper = requestMapper;
        this.responseMapper = responseMapper;
        this.requestTimeout = assistantProperties.getRequestTimeout();
        this.credentialPool = credentialPool == null
                ? new GeminiCredentialPool(geminiProperties, null, null) : credentialPool;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(assistantProperties.getRequestTimeout())
                .build();
    }

    @Override
    public AiProvider provider() {
        return AiProvider.GEMINI;
    }

    @Override
    public EmbeddingResult embed(EmbeddingRequest request, AiModelConfiguration configuration) {
        EmbeddingResult last = permanentFailure("EMBEDDING_CONFIG_INVALID");
        var deadline = credentialPool.deadline();
        int attempts = 0;
        for (var credential : credentialPool.ordered(GeminiCredentialPool.EMBEDDING)) {
            if (++attempts > Math.max(1, geminiProperties.getMaxAttempts())) break;
            try (var lease = credentialPool.acquire(credential, GeminiCredentialPool.EMBEDDING,
                    configuration.getModelId(), deadline)) {
                if (lease == null) { last = retryableFailure("EMBEDDING_CREDENTIALS_UNAVAILABLE"); continue; }
                last = send(request, configuration, credential, deadline);
                if (last.isSuccess()) {
                    credentialPool.success(credential, GeminiCredentialPool.EMBEDDING,
                            configuration.getModelId(), GeminiCredentialPool.EMBEDDING);
                    return last;
                }
                if (!last.isRetryable()) return last;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return permanentFailure("EMBEDDING_INTERRUPTED");
            }
        }
        return last;
    }

    private EmbeddingResult send(EmbeddingRequest request, AiModelConfiguration configuration,
            GeminiCredentialPool.Credential credential, java.time.Instant deadline) {
        try {
            String apiVersion = configuration.getApiVersion() == null || configuration.getApiVersion().isBlank()
                    ? "v1beta"
                    : configuration.getApiVersion();
            String endpoint = normalizedBaseUrl() + "/" + apiVersion + "/models/"
                    + configuration.getModelId() + ":embedContent";
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(credentialPool.timeout(deadline, requestTimeout))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", credential.secret())
                    .POST(HttpRequest.BodyPublishers.ofString(requestMapper.toEmbedContentBody(request, configuration)))
                    .build();
            HttpResponse<String> response = sendProvider(httpRequest, () -> credentialPool.sent(credential, GeminiCredentialPool.EMBEDDING, configuration.getModelId()));
            String body = credentialPool.sanitize(response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                var failure = credentialPool.failure(credential, GeminiCredentialPool.EMBEDDING,
                        configuration.getModelId(), response.statusCode(), body,
                        response.headers().firstValue("Retry-After").orElse(null));
                if (failure == GeminiCredentialPool.Failure.AUTH || failure == GeminiCredentialPool.Failure.MODEL) {
                    return retryableFailure("EMBEDDING_CREDENTIALS_UNAVAILABLE");
                }
            }
            int expectedDimension = configuration.getOutputDimension() == null ? 0 : configuration.getOutputDimension();
            return responseMapper.toEmbeddingResult(
                    response.statusCode(),
                    body,
                    retryAfterSeconds(response),
                    expectedDimension
            );
        } catch (java.net.http.HttpTimeoutException exception) {
            credentialPool.failure(credential, GeminiCredentialPool.EMBEDDING, configuration.getModelId(), 0, null, null);
            return retryableFailure("EMBEDDING_TIMEOUT");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return retryableFailure("EMBEDDING_NETWORK_ERROR");
        } catch (java.io.IOException exception) {
            credentialPool.failure(credential, GeminiCredentialPool.EMBEDDING, configuration.getModelId(), 0, null, null);
            return retryableFailure("EMBEDDING_NETWORK_ERROR");
        } catch (RuntimeException exception) {
            return permanentFailure("EMBEDDING_REQUEST_BUILD_FAILED");
        }
    }

    private EmbeddingResult retryableFailure(String code) {
        return EmbeddingResult.failure(new EmbeddingFailure(code, null, null, null, true, null));
    }

    private EmbeddingResult permanentFailure(String code) {
        return EmbeddingResult.failure(new EmbeddingFailure(code, null, null, null, false, null));
    }

    private Long retryAfterSeconds(HttpResponse<String> response) {
        Optional<String> header = response.headers().firstValue("Retry-After");
        if (header.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(header.get().trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String normalizedBaseUrl() {
        String baseUrl = geminiProperties.getBaseUrl();
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
