package com.ban.vehicle_management.infrastructure.ai.lmstudio;

import com.ban.vehicle_management.application.ai.port.out.AiProviderPortOut;
import com.ban.vehicle_management.application.ai.port.out.EmbeddingProviderPortOut;
import com.ban.vehicle_management.application.ai.service.AiRuntimeModeChangedException;
import com.ban.vehicle_management.domain.ai.model.*;
import com.ban.vehicle_management.infrastructure.ai.runtime.AiProviderHttpClient;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import org.springframework.stereotype.Component;

@Component
public class LmStudioProviderAdapter implements AiProviderPortOut, EmbeddingProviderPortOut {
    private final LmStudioProperties properties;
    private final LmStudioWireMapper mapper;
    private final ObjectMapper json;
    private final AiProviderHttpClient dispatch;
    private final HttpClient http;
    private final Semaphore inference = new Semaphore(1, true);

    public LmStudioProviderAdapter(LmStudioProperties properties, LmStudioWireMapper mapper,
            ObjectMapper json, AiProviderHttpClient dispatch) {
        this.properties = properties;
        this.mapper = mapper;
        this.json = json;
        this.dispatch = dispatch;
        this.http = HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }
    @Override public AiProvider provider() { return AiProvider.LM_STUDIO; }
    @Override public AiResponse generate(AiRequest request, AiModelConfiguration config) {
        if (!properties.getChatModel().equals(config.getModelId())) return AiResponse.failure("LOCAL_MODEL_NOT_APPROVED", false, null);
        if (request.tools() != null && !request.tools().isEmpty()) return AiResponse.failure("LOCAL_TOOL_UNSUPPORTED", false, null);
        int output = Math.min(properties.getMaxOutputTokens(), config.getMaxOutputTokens() == null ? 512 : config.getMaxOutputTokens());
        int bytes = bytes(request.systemInstruction());
        for (var message : request.messages()) bytes += bytes(message.content()) + 32;
        // UTF-8 bytes upper-bound tokenizer pieces; reserve framing/template tokens.
        int limit = Math.min(properties.getMaxInputBytes(), properties.getContextTokens()-output-256);
        if (bytes > limit || limit <= 0) return AiResponse.failure("LOCAL_CONTEXT_LIMIT", false, null);
        if (!inference.tryAcquire()) return AiResponse.failure("LOCAL_BUSY", false, null);
        try {
            var response = send("/chat/completions", json.writeValueAsString(mapper.chat(request, config, output)), properties.getGenerationTimeout());
            return mapper.answer(response.statusCode(), response.body());
        } catch (com.ban.vehicle_management.application.ai.service.AiLocalBusyException exception) { return AiResponse.failure("LOCAL_BUSY", false, null); }
        catch (AiRuntimeModeChangedException exception) { return AiResponse.failure("AI_MODE_CHANGED", false, null); }
        catch (HttpTimeoutException exception) { return AiResponse.failure("LOCAL_TIMEOUT", false, null); }
        catch (java.net.ConnectException exception) { return AiResponse.failure("LOCAL_SERVER_UNAVAILABLE", false, null); }
        catch (IllegalArgumentException exception) { return AiResponse.failure("LOCAL_ADDRESS_INVALID", false, null); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); return AiResponse.failure("LOCAL_INTERRUPTED", false, null); }
        catch (Exception exception) { return AiResponse.failure("LOCAL_UNAVAILABLE", false, null); }
        finally { inference.release(); }
    }
    @Override public EmbeddingResult embed(EmbeddingRequest request, AiModelConfiguration config) {
        if (!properties.getEmbeddingModel().equals(config.getModelId())) return LmStudioWireMapper.failure("LOCAL_MODEL_NOT_APPROVED", false);
        if (request == null || request.text() == null || bytes(request.text()) > 8000) return LmStudioWireMapper.failure("LOCAL_CONTEXT_LIMIT", false);
        if (!inference.tryAcquire()) return LmStudioWireMapper.failure("LOCAL_BUSY", false);
        try {
            var payload = json.createObjectNode().put("model", config.getModelId()).put("input", request.text())
                    .put("encoding_format", "float");
            var response = send("/embeddings", json.writeValueAsString(payload), properties.getEmbeddingTimeout());
            return mapper.embedding(response.statusCode(), response.body(), config.getModelId(),
                    config.getOutputDimension() == null ? 768 : config.getOutputDimension(), properties.isAllowQwenMatryoshka768() && config.getModelId().toLowerCase(java.util.Locale.ROOT).contains("qwen3-embedding-0.6b"));
        } catch (com.ban.vehicle_management.application.ai.service.AiLocalBusyException exception) { return LmStudioWireMapper.failure("LOCAL_BUSY", false); }
        catch (AiRuntimeModeChangedException exception) { return LmStudioWireMapper.failure("AI_MODE_CHANGED", false); }
        catch (HttpTimeoutException exception) { return LmStudioWireMapper.failure("LOCAL_TIMEOUT", false); }
        catch (java.net.ConnectException exception) { return LmStudioWireMapper.failure("LOCAL_SERVER_UNAVAILABLE", false); }
        catch (IllegalArgumentException exception) { return LmStudioWireMapper.failure("LOCAL_ADDRESS_INVALID", false); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); return LmStudioWireMapper.failure("LOCAL_INTERRUPTED", false); }
        catch (Exception exception) { return LmStudioWireMapper.failure("LOCAL_EMBEDDING_UNAVAILABLE", false); }
        finally { inference.release(); }
    }
    @Override public List<AiProviderModel> listModels() {
        try {
            var request = request("/models", properties.getConnectTimeout()).GET().build();
            var response = dispatch.send(provider(), http, request);
            if (response.statusCode() != 200) throw new IllegalStateException("LOCAL_CATALOG_UNAVAILABLE");
            var result = new ArrayList<AiProviderModel>();
            for (var model : json.readTree(response.body()).path("data")) {
                String id = model.path("id").asText();
                if (!Set.of(properties.getChatModel(), properties.getEmbeddingModel()).contains(id)) continue;
                boolean embedding = id.equals(properties.getEmbeddingModel());
                result.add(new AiProviderModel(provider(), id, id, "configured-local", embedding ? 2048 : properties.getContextTokens(),
                        embedding ? null : properties.getMaxOutputTokens(), List.of(embedding ? "embedContent" : "generateContent")));
            }
            return List.copyOf(result);
        } catch (AiRuntimeModeChangedException exception) { throw exception; }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException("LOCAL_CATALOG_INTERRUPTED"); }
        catch (Exception exception) { throw new IllegalStateException("LOCAL_CATALOG_UNAVAILABLE"); }
    }
    private HttpResponse<String> send(String path, String payload, Duration timeout) throws Exception {
        return dispatch.send(provider(), http, request(path, timeout).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8)).build());
    }
    private HttpRequest.Builder request(String path, Duration timeout) {
        URI base = URI.create(properties.getBaseUrl());
        if (!Set.of("http", "https").contains(base.getScheme()) || base.getHost() == null
                || !Set.of("127.0.0.1", "localhost", "host.docker.internal", "::1", "[::1]").contains(base.getHost())
                || base.getUserInfo() != null || base.getQuery() != null || base.getFragment() != null) {
            throw new IllegalArgumentException("LM Studio URL must use an approved local host");
        }
        var builder = HttpRequest.newBuilder(URI.create(properties.getBaseUrl().replaceAll("/+$", "") + path)).timeout(timeout);
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) builder.header("Authorization", "Bearer " + properties.getApiKey());
        return builder;
    }
    private int bytes(String value) { return value == null ? 0 : value.getBytes(StandardCharsets.UTF_8).length; }
}
