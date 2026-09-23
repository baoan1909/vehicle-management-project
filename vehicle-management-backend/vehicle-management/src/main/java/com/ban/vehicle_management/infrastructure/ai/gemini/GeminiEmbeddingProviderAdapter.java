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

@Component
public class GeminiEmbeddingProviderAdapter implements EmbeddingProviderPortOut {

    private final GeminiProperties geminiProperties;
    private final GeminiEmbeddingRequestMapper requestMapper;
    private final GeminiEmbeddingResponseMapper responseMapper;
    private final HttpClient httpClient;
    private final Duration requestTimeout;

    public GeminiEmbeddingProviderAdapter(
            GeminiProperties geminiProperties,
            AiAssistantProperties assistantProperties,
            GeminiEmbeddingRequestMapper requestMapper,
            GeminiEmbeddingResponseMapper responseMapper
    ) {
        this.geminiProperties = geminiProperties;
        this.requestMapper = requestMapper;
        this.responseMapper = responseMapper;
        this.requestTimeout = assistantProperties.getRequestTimeout();
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
        if (geminiProperties.getApiKey() == null || geminiProperties.getApiKey().isBlank()) {
            return EmbeddingResult.failure(new EmbeddingFailure(
                    "EMBEDDING_CONFIG_INVALID", null, null, null, false, null
            ));
        }
        try {
            String apiVersion = configuration.getApiVersion() == null || configuration.getApiVersion().isBlank()
                    ? "v1beta"
                    : configuration.getApiVersion();
            String endpoint = normalizedBaseUrl() + "/" + apiVersion + "/models/"
                    + configuration.getModelId() + ":embedContent";
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(requestTimeout)
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", geminiProperties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(requestMapper.toEmbedContentBody(request, configuration)))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            int expectedDimension = configuration.getOutputDimension() == null ? 0 : configuration.getOutputDimension();
            return responseMapper.toEmbeddingResult(
                    response.statusCode(),
                    response.body(),
                    retryAfterSeconds(response),
                    expectedDimension
            );
        } catch (java.net.http.HttpTimeoutException exception) {
            return retryableFailure("EMBEDDING_TIMEOUT");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return retryableFailure("EMBEDDING_NETWORK_ERROR");
        } catch (java.io.IOException exception) {
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
