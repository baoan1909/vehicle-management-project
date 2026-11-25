package com.ban.vehicle_management.infrastructure.ai.lmstudio;

import com.ban.vehicle_management.domain.ai.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

/** OpenAI-compatible wire mapping; never accepts a provider's answer as trusted. */
@Component
public class LmStudioWireMapper {
    private final ObjectMapper json;
    public LmStudioWireMapper(ObjectMapper json) { this.json = json; }

    public ObjectNode chat(AiRequest request, AiModelConfiguration config, int maxTokens) throws Exception {
        ObjectNode root = json.createObjectNode();
        root.put("model", config.getModelId());
        root.put("stream", false);
        root.put("max_tokens", maxTokens);
        root.put("temperature", config.getTemperature() == null ? 0.3 : config.getTemperature().doubleValue());
        root.put("reasoning_effort", "none");
        root.putObject("chat_template_kwargs").put("enable_thinking", false);
        var messages = root.putArray("messages");
        messages.addObject().put("role", "system").put("content", request.systemInstruction());
        for (var message : request.messages()) {
            if (!java.util.Set.of("user", "assistant", "system").contains(message.role())) {
                throw new IllegalArgumentException("Unsupported chat role");
            }
            messages.addObject().put("role", message.role()).put("content", message.content());
        }
        if (request.structuredOutput() && request.responseJsonSchema() != null) {
            var format = root.putObject("response_format");
            format.put("type", "json_schema");
            format.putObject("json_schema").put("name", "coparking_grounded_answer")
                    .put("strict", true).set("schema", json.readTree(request.responseJsonSchema()));
        }
        return root;
    }
    public AiResponse answer(int status, String body) {
        if (status < 200 || status >= 300) return AiResponse.failure(errorCode(status, body), false, null);
        try {
            JsonNode root = json.readTree(body);
            if (root.path("choices").size() != 1) return AiResponse.failure("INVALID_RESPONSE_SCHEMA", false, null);
            var choice = root.path("choices").get(0);
            if (!"stop".equals(choice.path("finish_reason").asText())) {
                return AiResponse.failure("LOCAL_RESPONSE_TRUNCATED", false, null);
            }
            var message = choice.path("message");
            if (!message.path("tool_calls").isEmpty()) return AiResponse.failure("LOCAL_TOOL_UNSUPPORTED", false, null);
            String text = message.path("content").asText("");
            if (text.isBlank() || text.contains("<think>") || text.contains("</think>")) {
                return AiResponse.failure("INVALID_RESPONSE_SCHEMA", false, null);
            }
            return AiResponse.success(text, null, nullableInt(root.path("usage").get("prompt_tokens")),
                    nullableInt(root.path("usage").get("completion_tokens")));
        } catch (Exception exception) { return AiResponse.failure("INVALID_RESPONSE_SCHEMA", false, null); }
    }
    public EmbeddingResult embedding(int status, String body, String model, int dimension, boolean allowMrl) {
        if (status < 200 || status >= 300) return failure(errorCode(status, body), false);
        try {
            JsonNode root = json.readTree(body);
            if (root.path("data").size() != 1) return failure("EMBEDDING_INVALID_RESPONSE", false);
            JsonNode values = root.path("data").get(0).path("embedding");
            // Qwen3-Embedding documents MRL support. Only this explicit 1024 -> 768
            // projection is supported, then L2-normalized identically for query/docs.
            boolean mrl = allowMrl && dimension == 768 && values.size() == 1024;
            if (!values.isArray() || (values.size() != dimension && !mrl)) {
                return failure("EMBEDDING_DIMENSION_MISMATCH", false);
            }
            for (JsonNode value : values) {
                if (!value.isNumber() || !Double.isFinite(value.asDouble())) return failure("EMBEDDING_VECTOR_INVALID", false);
            }
            double[] vector = new double[dimension];
            double norm = 0;
            for (int i=0; i<dimension; i++) { vector[i] = values.get(i).asDouble(); norm += vector[i]*vector[i]; }
            if (!(norm>0) || !Double.isFinite(norm)) return failure("EMBEDDING_VECTOR_INVALID", false);
            norm = Math.sqrt(norm);
            for (int i=0; i<dimension; i++) vector[i] /= norm;
            return EmbeddingResult.success(EmbeddingVector.of(vector, dimension), model);
        } catch (Exception exception) { return failure("EMBEDDING_INVALID_RESPONSE", false); }
    }
    private Integer nullableInt(JsonNode value) { return value != null && value.canConvertToInt() ? value.asInt() : null; }
    public static String errorCode(int status, String body) {
        if (status == 401 || status == 403) return "LOCAL_AUTH_FAILED";
        String detail = body == null ? "" : body.toLowerCase(java.util.Locale.ROOT);
        if (detail.contains("out of memory") || detail.contains("insufficient memory") || detail.contains("failed to allocate")) return "LOCAL_OUT_OF_MEMORY";
        if (detail.contains("context length") || detail.contains("context window") || detail.contains("too many tokens")) return "LOCAL_CONTEXT_LIMIT";
        if (detail.contains("not loaded") || detail.contains("no models loaded") || detail.contains("model is loading")) return "LOCAL_MODEL_NOT_READY";
        return errorCode(status);
    }

    public static String errorCode(int status) {
        return switch(status) {
            case 401, 403 -> "LOCAL_AUTH_FAILED";
            case 404 -> "MODEL_NOT_FOUND";
            case 400, 422 -> "LOCAL_REQUEST_REJECTED";
            case 429 -> "HTTP_429";
            default -> status >= 500 ? "HTTP_5XX" : "LOCAL_PROVIDER_ERROR";
        };
    }
    public static EmbeddingResult failure(String code, boolean retryable) {
        return EmbeddingResult.failure(new EmbeddingFailure(code, null, null, null, retryable, null));
    }
}
