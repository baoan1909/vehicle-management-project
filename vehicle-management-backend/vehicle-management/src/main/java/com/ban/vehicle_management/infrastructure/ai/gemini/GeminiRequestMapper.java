package com.ban.vehicle_management.infrastructure.ai.gemini;

import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.AiRequest;
import com.ban.vehicle_management.domain.ai.model.AiRequestMessage;
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
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        BigDecimal temperature = configuration.getTemperature();
        if (temperature != null && supportsSamplingParameters(configuration)) {
            generationConfig.put("temperature", temperature);
        }
        if (configuration.getMaxOutputTokens() != null) {
            generationConfig.put("maxOutputTokens", configuration.getMaxOutputTokens());
        }
        if (request.structuredOutput() || configuration.isRequiresStructuredOutput()) {
            generationConfig.put("responseMimeType", "application/json");
        }
        body.put("generationConfig", generationConfig);
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not build Gemini request");
        }
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
}
