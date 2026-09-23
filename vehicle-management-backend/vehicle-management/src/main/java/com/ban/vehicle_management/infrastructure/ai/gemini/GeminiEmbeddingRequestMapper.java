package com.ban.vehicle_management.infrastructure.ai.gemini;

import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.EmbeddingRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GeminiEmbeddingRequestMapper {

    private final ObjectMapper objectMapper;

    public GeminiEmbeddingRequestMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toEmbedContentBody(EmbeddingRequest request, AiModelConfiguration configuration) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("content", Map.of("parts", List.of(Map.of("text", request.text()))));
        if (configuration.getOutputDimension() == null || configuration.getOutputDimension() != 768) {
            throw new IllegalArgumentException("Gemini embedding output dimension phải là 768");
        }
        body.put("output_dimensionality", configuration.getOutputDimension());
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not build Gemini embedContent request");
        }
    }
}
