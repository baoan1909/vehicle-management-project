package com.ban.vehicle_management.infrastructure.ai.gemini;

import com.ban.vehicle_management.domain.ai.model.AiFunctionCall;
import com.ban.vehicle_management.domain.ai.model.AiProviderErrorDetail;
import com.ban.vehicle_management.domain.ai.model.AiProviderModel;
import com.ban.vehicle_management.domain.ai.model.AiResponse;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GeminiResponseMapper {

    private final ObjectMapper objectMapper;

    public GeminiResponseMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AiResponse toAiResponse(int statusCode, String body) {
        if (statusCode < 200 || statusCode >= 300) {
            return toFailureResponse(statusCode, body);
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.path("promptFeedback").path("blockReason").isTextual()) {
                return AiResponse.safetyBlocked(body, null, null);
            }
            StringBuilder text = new StringBuilder();
            JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
            String finishReason = textOrNull(root.path("candidates").path(0).path("finishReason"));
            AiFunctionCall functionCall = null;
            if (parts.isArray()) {
                for (JsonNode part : parts) {
                    JsonNode textNode = part.get("text");
                    if (textNode != null && textNode.isTextual()) {
                        text.append(textNode.asText());
                    }
                    JsonNode functionCallNode = part.get("functionCall");
                    if (functionCallNode != null && functionCallNode.isObject()) {
                        functionCall = toFunctionCall(functionCallNode, textOrNull(part.get("thoughtSignature")));
                    }
                }
            }
            Integer inputTokens = intOrNull(root.path("usageMetadata").path("promptTokenCount"));
            Integer outputTokens = intOrNull(root.path("usageMetadata").path("candidatesTokenCount"));
            if (functionCall != null) {
                return AiResponse.functionCall(functionCall, body, inputTokens, outputTokens, finishReason);
            }
            return AiResponse.success(text.toString(), body, inputTokens, outputTokens);
        } catch (Exception exception) {
            return AiResponse.failure("INVALID_RESPONSE_SCHEMA", true, body);
        }
    }

    private AiResponse toFailureResponse(int statusCode, String body) {
        boolean retryable = isRetryableStatus(statusCode);
        String failureCode = statusCode == 404 ? "MODEL_NOT_FOUND" : normalizedHttpFailureCode(statusCode);
        AiProviderErrorDetail detail = parseProviderError(statusCode, body, retryable);
        return AiResponse.failure(failureCode, retryable, null, detail);
    }

    private boolean isRetryableStatus(int statusCode) {
        return statusCode == 429 || statusCode >= 500;
    }

    private String normalizedHttpFailureCode(int statusCode) {
        if (statusCode >= 500) {
            return "HTTP_5XX";
        }
        return "HTTP_" + statusCode;
    }

    private AiProviderErrorDetail parseProviderError(int statusCode, String body, boolean retryable) {
        try {
            JsonNode error = objectMapper.readTree(body == null || body.isBlank() ? "{}" : body).path("error");
            Integer providerStatus = intOrNull(error.path("code"));
            String providerErrorCode = sanitize(textOrNull(error.path("status")), 80);
            String providerErrorMessage = sanitize(textOrNull(error.path("message")), 500);
            return new AiProviderErrorDetail(
                    providerStatus == null ? statusCode : providerStatus,
                    providerErrorCode,
                    providerErrorMessage,
                    fieldViolations(error.path("details")),
                    retryable
            );
        } catch (Exception exception) {
            return new AiProviderErrorDetail(statusCode, null, null, "[]", retryable);
        }
    }

    private String fieldViolations(JsonNode details) {
        List<java.util.Map<String, String>> violations = new ArrayList<>();
        if (details != null && details.isArray()) {
            for (JsonNode detail : details) {
                JsonNode fieldViolations = detail.path("fieldViolations");
                if (!fieldViolations.isArray()) {
                    fieldViolations = detail.path("badRequest").path("fieldViolations");
                }
                if (!fieldViolations.isArray()) {
                    continue;
                }
                for (JsonNode violation : fieldViolations) {
                    String field = sanitize(textOrNull(violation.path("field")), 240);
                    String description = sanitize(textOrNull(violation.path("description")), 500);
                    if (field != null || description != null) {
                        java.util.Map<String, String> item = new java.util.LinkedHashMap<>();
                        if (field != null) {
                            item.put("field", field);
                        }
                        if (description != null) {
                            item.put("description", description);
                        }
                        violations.add(item);
                    }
                }
            }
        }
        try {
            return objectMapper.writeValueAsString(violations);
        } catch (Exception exception) {
            return "[]";
        }
    }

    private String sanitize(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String sanitized = value
                .replaceAll("(?i)(x-goog-api-key|api[-_ ]?key|authorization|bearer|token)\\s*[:=]\\s*[^\\s,;]+", "$1=[REDACTED]")
                .replace('<', '[')
                .replace('>', ']')
                .trim();
        return sanitized.length() <= maxLength ? sanitized : sanitized.substring(0, maxLength);
    }

    private AiFunctionCall toFunctionCall(JsonNode node, String partThoughtSignature) {
        String name = textOrNull(node.path("name"));
        JsonNode argsNode = node.path("args");
        String thoughtSignature = textOrNull(node.path("thoughtSignature"));
        if (thoughtSignature == null) {
            thoughtSignature = partThoughtSignature;
        }
        if (name == null || name.isBlank() || !argsNode.isObject()) {
            return new AiFunctionCall(name, "{}", true, "MALFORMED_FUNCTION_CALL", null, thoughtSignature);
        }
        try {
            return new AiFunctionCall(
                    name, objectMapper.writeValueAsString(argsNode), false, null, null, thoughtSignature);
        } catch (Exception exception) {
            return new AiFunctionCall(name, "{}", true, "MALFORMED_FUNCTION_CALL", null, thoughtSignature);
        }
    }

    public List<AiProviderModel> toProviderModels(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            List<AiProviderModel> models = new ArrayList<>();
            JsonNode modelNodes = root.path("models");
            if (!modelNodes.isArray()) {
                return List.of();
            }
            for (JsonNode node : modelNodes) {
                String rawName = textOrNull(node.path("name"));
                if (rawName == null) {
                    continue;
                }
                String modelId = rawName.startsWith("models/") ? rawName.substring("models/".length()) : rawName;
                models.add(new AiProviderModel(
                        AiProvider.GEMINI,
                        modelId,
                        textOrNull(node.path("displayName")),
                        textOrNull(node.path("version")),
                        intOrNull(node.path("inputTokenLimit")),
                        intOrNull(node.path("outputTokenLimit")),
                        strings(node.path("supportedGenerationMethods"))
                ));
            }
            return models;
        } catch (Exception exception) {
            return List.of();
        }
    }

    public String nextPageToken(String body) {
        try {
            return textOrNull(objectMapper.readTree(body).path("nextPageToken"));
        } catch (Exception exception) {
            return null;
        }
    }

    private Integer intOrNull(JsonNode node) {
        return node != null && node.canConvertToInt() ? node.asInt() : null;
    }

    private String textOrNull(JsonNode node) {
        return node != null && node.isTextual() ? node.asText() : null;
    }

    private List<String> strings(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        node.forEach(item -> {
            if (item.isTextual()) {
                values.add(item.asText());
            }
        });
        return values;
    }
}
