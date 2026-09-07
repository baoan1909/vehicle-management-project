package com.ban.vehicle_management.infrastructure.ai.gemini;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.AiRequest;
import com.ban.vehicle_management.domain.ai.model.AiRequestMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeminiRequestMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GeminiRequestMapper mapper = new GeminiRequestMapper(objectMapper);

    @Test
    void shouldSkipDeprecatedSamplingParametersForGemini3() throws Exception {
        AiModelConfiguration configuration = configuration("gemini-3.6-flash");

        JsonNode body = objectMapper.readTree(mapper.toGenerateContentBody(request(), configuration));

        assertFalse(body.get("generationConfig").has("temperature"));
        assertEquals(1024, body.get("generationConfig").get("maxOutputTokens").asInt());
        assertEquals("application/json", body.get("generationConfig").get("responseMimeType").asText());
    }

    @Test
    void shouldKeepTemperatureForOlderGeminiModels() throws Exception {
        AiModelConfiguration configuration = configuration("gemini-2.5-flash");

        JsonNode body = objectMapper.readTree(mapper.toGenerateContentBody(request(), configuration));

        assertTrue(body.get("generationConfig").has("temperature"));
        assertEquals(0, new BigDecimal("0.30").compareTo(body.get("generationConfig").get("temperature").decimalValue()));
    }

    private AiRequest request() {
        return new AiRequest(
                "Answer in Vietnamese",
                List.of(new AiRequestMessage("user", "Xin chao")),
                true
        );
    }

    private AiModelConfiguration configuration(String modelId) {
        AiModelConfiguration configuration = new AiModelConfiguration();
        configuration.setModelId(modelId);
        configuration.setTemperature(new BigDecimal("0.30"));
        configuration.setMaxOutputTokens(1024);
        configuration.setRequiresStructuredOutput(true);
        return configuration;
    }
}
