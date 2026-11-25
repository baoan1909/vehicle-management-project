package com.ban.vehicle_management.infrastructure.ai.gemini;

import com.ban.vehicle_management.application.ai.port.out.AiProviderPortOut;
import com.ban.vehicle_management.application.ai.service.AiAssistantProperties;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.AiProviderModel;
import com.ban.vehicle_management.domain.ai.model.AiRequest;
import com.ban.vehicle_management.domain.ai.model.AiResponse;
import com.ban.vehicle_management.domain.ai.model.AiProviderAttempt;
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
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class GeminiAiProviderAdapter implements AiProviderPortOut {

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
    private final AiAssistantProperties assistantProperties;
    private final GeminiRequestMapper requestMapper;
    private final GeminiResponseMapper responseMapper;
    private final HttpClient httpClient;
    private final GeminiCredentialPool credentialPool;

    public GeminiAiProviderAdapter(
            GeminiProperties geminiProperties,
            AiAssistantProperties assistantProperties,
            GeminiRequestMapper requestMapper,
            GeminiResponseMapper responseMapper
    ) {
        this(geminiProperties, assistantProperties, requestMapper, responseMapper, null);
    }

    @Autowired
    public GeminiAiProviderAdapter(GeminiProperties geminiProperties,
            AiAssistantProperties assistantProperties, GeminiRequestMapper requestMapper,
            GeminiResponseMapper responseMapper, GeminiCredentialPool credentialPool) {
        this.geminiProperties = geminiProperties;
        this.assistantProperties = assistantProperties;
        this.requestMapper = requestMapper;
        this.responseMapper = responseMapper;
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
    public AiResponse generate(AiRequest request, AiModelConfiguration configuration) {
        AiResponse last = AiResponse.failure("GEMINI_API_KEY_MISSING", false, null);
        for (AiProviderAttempt attempt : generationAttempts(List.of(configuration))) {
            last = generateAttempt(request, attempt);
            if (last.success() || last.safetyBlocked() || !last.retryable()) return last;
        }
        return last;
    }

    @Override
    public boolean managesAvailability() { return true; }

    @Override
    public List<AiProviderAttempt> generationAttempts(List<AiModelConfiguration> configurations) {
        List<AiProviderAttempt> planned = credentialPool.generationAttempts(configurations);
        if (planned.isEmpty() && !configurations.isEmpty()) return List.of(new AiProviderAttempt(
                configurations.getFirst(), "unavailable", credentialPool.deadline()));
        return planned;
    }

    @Override
    public AiResponse generateAttempt(AiRequest request, AiProviderAttempt attempt) {
        AiModelConfiguration configuration = attempt.configuration();
        var credential = credentialPool.credential(attempt.credentialAlias()).orElse(null);
        if (credential == null) return credentialPool.credentials().isEmpty()
                ? AiResponse.failure("GEMINI_API_KEY_MISSING", false, null)
                : AiResponse.failure("GEMINI_CANDIDATE_UNAVAILABLE", true, null);
        String model = configuration.getModelId();
        try (var lease = credentialPool.acquire(credential, GeminiCredentialPool.GENERATION, model, attempt.deadline())) {
            if (lease == null) return AiResponse.failure("GEMINI_CANDIDATE_UNAVAILABLE", true, null);
            return send(request, configuration, credential, attempt.deadline());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return AiResponse.failure("INTERRUPTED", false, null);
        }
    }

    private AiResponse send(AiRequest request, AiModelConfiguration configuration,
            GeminiCredentialPool.Credential credential, java.time.Instant deadline) {
        java.time.Duration configuredTimeout = configuration.getUseCase() == com.ban.vehicle_management.shared.enumeration.ai.AiUseCase.SUPPORT_CHAT
                ? assistantProperties.getGenerationTimeout() : assistantProperties.getRequestTimeout();
        java.time.Duration allottedTimeout = credentialPool.timeout(deadline, configuredTimeout);
        try {
            String apiVersion = configuration.getApiVersion() == null || configuration.getApiVersion().isBlank()
                    ? "v1beta"
                    : configuration.getApiVersion();
            String endpoint = normalizedBaseUrl() + "/" + apiVersion + "/models/"
                    + configuration.getModelId() + ":generateContent";
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(allottedTimeout)
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", credential.secret())
                    .POST(HttpRequest.BodyPublishers.ofString(requestMapper.toGenerateContentBody(request, configuration)))
                    .build();
            HttpResponse<String> response = sendProvider(httpRequest, () -> credentialPool.sent(credential, GeminiCredentialPool.GENERATION, configuration.getModelId()));
            String body = credentialPool.sanitize(response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                var failure = credentialPool.failure(credential, GeminiCredentialPool.GENERATION,
                        configuration.getModelId(), response.statusCode(), body,
                        response.headers().firstValue("Retry-After").orElse(null));
                if (failure == GeminiCredentialPool.Failure.AUTH || failure == GeminiCredentialPool.Failure.MODEL) {
                    return AiResponse.failure("GEMINI_CANDIDATE_UNAVAILABLE", true, null);
                }
            }
            AiResponse result = responseMapper.toAiResponse(response.statusCode(), body);
            if (result.success()) credentialPool.success(credential, GeminiCredentialPool.GENERATION,
                    configuration.getModelId(), GeminiCredentialPool.GENERATION + ":" + configuration.getUseCase());
            return result;
        } catch (java.net.http.HttpTimeoutException exception) {
            // Small budget drift from constructing/acquiring the request does not turn
            // an effectively full timeout into a healthy skipped model. A genuinely
            // short fallback slice (under 90% of its timeout) stays eligible.
            if (allottedTimeout.compareTo(configuredTimeout.multipliedBy(9).dividedBy(10)) < 0) {
                // The remaining shared budget is not a fair model timeout. Do not
                // disable a healthy fallback merely because its time slice was short.
                return AiResponse.failure("GEMINI_REQUEST_BUDGET_EXHAUSTED", true, null);
            }
            credentialPool.failure(credential, GeminiCredentialPool.GENERATION, configuration.getModelId(), 0, null, null);
            return AiResponse.failure("TIMEOUT", true, null);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return AiResponse.failure("INTERRUPTED", true, null);
        } catch (java.io.IOException exception) {
            credentialPool.failure(credential, GeminiCredentialPool.GENERATION, configuration.getModelId(), 0, null, null);
            return AiResponse.failure("PROVIDER_ERROR", true, null);
        } catch (RuntimeException exception) {
            return AiResponse.failure("GEMINI_REQUEST_BUILD_FAILED", false, null);
        }
    }

    @Override
    public List<AiProviderModel> listModels() {
        java.time.Instant deadline = credentialPool.deadline();
        for (var credential : credentialPool.ordered("listModels")) {
            try (var lease = credentialPool.acquire(credential, "listModels", "catalog", deadline)) {
                if (lease == null) continue;
                return listModels(credential, deadline);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("GEMINI_LIST_MODELS_INTERRUPTED");
            }
            catch (IllegalStateException ignored) { /* The next configured credential may be healthy. */ }
        }
        throw new IllegalStateException("GEMINI_LIST_MODELS_UNAVAILABLE");
    }

    private List<AiProviderModel> listModels(GeminiCredentialPool.Credential credential, java.time.Instant deadline) {
        try {
            List<AiProviderModel> models = new ArrayList<>();
            String pageToken = null;
            java.util.Set<String> seenTokens = new java.util.HashSet<>();
            do {
                if (!java.time.Instant.now().isBefore(deadline)) throw new IllegalStateException("GEMINI_LIST_MODELS_DEADLINE");
                String endpoint = normalizedBaseUrl() + "/v1beta/models?pageSize=1000"
                        + (pageToken == null ? "" : "&pageToken=" + URLEncoder.encode(pageToken, StandardCharsets.UTF_8));
                HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint))
                        .timeout(credentialPool.timeout(deadline, assistantProperties.getRequestTimeout()))
                        .header("x-goog-api-key", credential.secret())
                        .GET()
                        .build();
                HttpResponse<String> response = sendProvider(httpRequest, () -> credentialPool.sent(credential, "listModels", "catalog"));
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    credentialPool.failure(credential, "listModels", "catalog", response.statusCode(),
                            credentialPool.sanitize(response.body()), response.headers().firstValue("Retry-After").orElse(null));
                    throw new IllegalStateException("GEMINI_LIST_MODELS_HTTP_" + response.statusCode());
                }
                String body = credentialPool.sanitize(response.body());
                models.addAll(responseMapper.toProviderModels(body));
                pageToken = responseMapper.nextPageToken(body);
                if (pageToken != null && (!seenTokens.add(pageToken) || seenTokens.size() > 100)) {
                    throw new IllegalStateException("GEMINI_LIST_MODELS_PAGINATION_INVALID");
                }
            } while (pageToken != null && !pageToken.isBlank());
            return models;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("GEMINI_LIST_MODELS_INTERRUPTED");
        } catch (Exception exception) {
            throw new IllegalStateException("GEMINI_LIST_MODELS_FAILED");
        }
    }

    private String normalizedBaseUrl() {
        String baseUrl = geminiProperties.getBaseUrl();
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
