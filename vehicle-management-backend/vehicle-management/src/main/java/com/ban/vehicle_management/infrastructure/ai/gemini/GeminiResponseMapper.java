package com.ban.vehicle_management.infrastructure.ai.gemini;

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
        if (statusCode == 429) {
            return AiResponse.failure("HTTP_429", true, null);
        }
        if (statusCode == 404) {
            return AiResponse.failure("MODEL_NOT_FOUND", true, null);
        }
        if (statusCode >= 500) {
            return AiResponse.failure("HTTP_5XX", true, null);
        }
        if (statusCode == 400 || statusCode == 403) {
            return AiResponse.failure("HTTP_" + statusCode, false, null);
        }
        if (statusCode < 200 || statusCode >= 300) {
            return AiResponse.failure("HTTP_" + statusCode, false, null);
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            StringBuilder text = new StringBuilder();
            JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
            if (parts.isArray()) {
                for (JsonNode part : parts) {
                    JsonNode textNode = part.get("text");
                    if (textNode != null && textNode.isTextual()) {
                        text.append(textNode.asText());
                    }
                }
            }
            Integer inputTokens = intOrNull(root.path("usageMetadata").path("promptTokenCount"));
            Integer outputTokens = intOrNull(root.path("usageMetadata").path("candidatesTokenCount"));
            return AiResponse.success(text.toString(), null, inputTokens, outputTokens);
        } catch (Exception exception) {
            return AiResponse.failure("INVALID_RESPONSE_SCHEMA", true, null);
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
