package com.ban.vehicle_management.infrastructure.ai.gemini;

import com.ban.vehicle_management.application.ai.port.out.AiProviderPortOut;
import com.ban.vehicle_management.application.ai.service.AiAssistantProperties;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.AiProviderModel;
import com.ban.vehicle_management.domain.ai.model.AiRequest;
import com.ban.vehicle_management.domain.ai.model.AiResponse;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GeminiAiProviderAdapter implements AiProviderPortOut {

    private final GeminiProperties geminiProperties;
    private final AiAssistantProperties assistantProperties;
    private final GeminiRequestMapper requestMapper;
    private final GeminiResponseMapper responseMapper;
    private final HttpClient httpClient;

    public GeminiAiProviderAdapter(
            GeminiProperties geminiProperties,
            AiAssistantProperties assistantProperties,
            GeminiRequestMapper requestMapper,
            GeminiResponseMapper responseMapper
    ) {
        this.geminiProperties = geminiProperties;
        this.assistantProperties = assistantProperties;
        this.requestMapper = requestMapper;
        this.responseMapper = responseMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(assistantProperties.getRequestTimeout())
                .build();
    }

    @Override
    public AiProvider provider() {
        return AiProvider.GEMINI;
    }

    @Override
    public AiResponse generate(AiRequest request, AiModelConfiguration configuration) {
        if (geminiProperties.getApiKey() == null || geminiProperties.getApiKey().isBlank()) {
            return AiResponse.failure("GEMINI_API_KEY_MISSING", false, null);
        }
        try {
            String apiVersion = configuration.getApiVersion() == null || configuration.getApiVersion().isBlank()
                    ? "v1beta"
                    : configuration.getApiVersion();
            String endpoint = normalizedBaseUrl() + "/" + apiVersion + "/models/"
                    + configuration.getModelId() + ":generateContent";
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(assistantProperties.getRequestTimeout())
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", geminiProperties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(requestMapper.toGenerateContentBody(request, configuration)))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            return responseMapper.toAiResponse(response.statusCode(), response.body());
        } catch (java.net.http.HttpTimeoutException exception) {
            return AiResponse.failure("TIMEOUT", true, null);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return AiResponse.failure("INTERRUPTED", true, null);
        } catch (Exception exception) {
            return AiResponse.failure("PROVIDER_ERROR", true, null);
        }
    }

    @Override
    public List<AiProviderModel> listModels() {
        if (geminiProperties.getApiKey() == null || geminiProperties.getApiKey().isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY_MISSING");
        }
        try {
            List<AiProviderModel> models = new ArrayList<>();
            String pageToken = null;
            do {
                String endpoint = normalizedBaseUrl() + "/v1beta/models?pageSize=1000"
                        + (pageToken == null ? "" : "&pageToken=" + URLEncoder.encode(pageToken, StandardCharsets.UTF_8));
                HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint))
                        .timeout(assistantProperties.getRequestTimeout())
                        .header("x-goog-api-key", geminiProperties.getApiKey())
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IllegalStateException("GEMINI_LIST_MODELS_HTTP_" + response.statusCode());
                }
                models.addAll(responseMapper.toProviderModels(response.body()));
                pageToken = responseMapper.nextPageToken(response.body());
            } while (pageToken != null && !pageToken.isBlank());
            return models;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("GEMINI_LIST_MODELS_INTERRUPTED");
        } catch (Exception exception) {
            throw new IllegalStateException("GEMINI_LIST_MODELS_FAILED", exception);
        }
    }

    private String normalizedBaseUrl() {
        String baseUrl = geminiProperties.getBaseUrl();
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
