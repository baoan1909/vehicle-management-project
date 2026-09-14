package com.ban.vehicle_management.infrastructure.ai.gemini;

import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.AiFunctionResponse;
import com.ban.vehicle_management.domain.ai.model.AiRequest;
import com.ban.vehicle_management.domain.ai.model.AiRequestMessage;
import com.ban.vehicle_management.domain.ai.model.AiToolDeclaration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GeminiRequestMapper {

    private final ObjectMapper objectMapper;

    public GeminiRequestMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toGenerateContentBody(AiRequest request, AiModelConfiguration configuration) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (request.systemInstruction() != null && !request.systemInstruction().isBlank()) {
            body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", request.systemInstruction()))));
        }
        body.put("contents", toContents(request.messages()));
        appendFunctionResponses(body, request.functionResponses());
        boolean usesToolProtocol = hasItems(request.tools()) || hasItems(request.functionResponses());
        if (hasItems(request.tools())) {
            body.put("tools", List.of(Map.of("functionDeclarations", toFunctionDeclarations(request.tools()))));
        }
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        BigDecimal temperature = configuration.getTemperature();
        if (temperature != null && supportsSamplingParameters(configuration)) {
            generationConfig.put("temperature", temperature);
        }
        if (configuration.getMaxOutputTokens() != null) {
            generationConfig.put("maxOutputTokens", configuration.getMaxOutputTokens());
        }
        if (!usesToolProtocol && (request.structuredOutput() || configuration.isRequiresStructuredOutput())) {
            generationConfig.put("responseMimeType", "application/json");
        }
        body.put("generationConfig", generationConfig);
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not build Gemini request");
        }
    }

    private boolean hasItems(List<?> items) {
        return items != null && !items.isEmpty();
    }

    private boolean supportsSamplingParameters(AiModelConfiguration configuration) {
        if (configuration == null || configuration.getModelId() == null) {
            return true;
        }
        String modelId = configuration.getModelId().toLowerCase();
        return !modelId.startsWith("gemini-3.") && !modelId.contains("/gemini-3.");
    }

    private List<Map<String, Object>> toContents(List<AiRequestMessage> messages) {
        List<Map<String, Object>> contents = new ArrayList<>();
        if (messages == null || messages.isEmpty()) {
            contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", "Xin chao"))));
            return contents;
        }
        for (AiRequestMessage message : messages) {
            String role = "model".equals(message.role()) ? "model" : "user";
            String text = message.content() == null ? "" : message.content();
            if (!text.isBlank()) {
                contents.add(Map.of("role", role, "parts", List.of(Map.of("text", text))));
            }
        }
        return contents;
    }

    @SuppressWarnings("unchecked")
    private void appendFunctionResponses(Map<String, Object> body, List<AiFunctionResponse> functionResponses) {
        if (functionResponses == null || functionResponses.isEmpty()) {
            return;
        }
        List<Map<String, Object>> contents = (List<Map<String, Object>>) body.get("contents");
        for (AiFunctionResponse response : functionResponses) {
            contents.add(Map.of(
                    "role", "user",
                    "parts", List.of(Map.of(
                            "functionResponse", Map.of(
                                    "name", response.name(),
                                    "response", readObject(response.responseJson())
                            )
                    ))
            ));
        }
    }

    private List<Map<String, Object>> toFunctionDeclarations(List<AiToolDeclaration> declarations) {
        return declarations.stream()
                .map(declaration -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", declaration.name());
                    item.put("description", declaration.description());
                    item.put("parametersJsonSchema", readObject(declaration.parametersSchema()));
                    return item;
                })
                .toList();
    }

    private Object readObject(String json) {
        try {
            JsonNode node = objectMapper.readTree(json == null || json.isBlank() ? "{}" : json);
            return objectMapper.convertValue(node, Object.class);
        } catch (Exception exception) {
            return Map.of();
        }
    }
}
