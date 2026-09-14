package com.ban.vehicle_management.infrastructure.ai.gemini;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.AiFunctionResponse;
import com.ban.vehicle_management.domain.ai.model.AiRequest;
import com.ban.vehicle_management.domain.ai.model.AiRequestMessage;
import com.ban.vehicle_management.domain.ai.model.AiToolDeclaration;
import com.ban.vehicle_management.shared.enumeration.ai.AiToolType;
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

    @Test
    void shouldMapToolDeclarationsAndFunctionResponses() throws Exception {
        AiModelConfiguration configuration = configuration("gemini-2.5-flash");
        AiRequest request = new AiRequest(
                "Answer in Vietnamese",
                List.of(new AiRequestMessage("user", "Tim huong dan dang ky ve thang")),
                true,
                List.of(new AiToolDeclaration(
                        "search_support_knowledge",
                        "Search support knowledge",
                        "{\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\"}},\"additionalProperties\":false,\"required\":[\"query\"]}",
                        AiToolType.READ_ONLY,
                        null,
                        false
                ), new AiToolDeclaration(
                        "list_my_support_tickets",
                        "List support tickets",
                        "{\"type\":\"object\",\"properties\":{\"status\":{\"type\":\"string\"}},\"additionalProperties\":false}",
                        AiToolType.READ_ONLY,
                        null,
                        false
                )),
                List.of(new AiFunctionResponse(
                        "search_support_knowledge",
                        "{\"results\":[{\"title\":\"Dang ky ve thang\"}]}"
                ))
        );

        JsonNode body = objectMapper.readTree(mapper.toGenerateContentBody(request, configuration));

        JsonNode declaration = body.get("tools").get(0).get("functionDeclarations").get(0);
        assertEquals("search_support_knowledge", declaration.get("name").asText());
        assertFalse(declaration.has("parameters"));
        JsonNode parametersJsonSchema = declaration.get("parametersJsonSchema");
        assertEquals("object", parametersJsonSchema.get("type").asText());
        assertFalse(parametersJsonSchema.get("additionalProperties").asBoolean());
        assertEquals("query", parametersJsonSchema.get("required").get(0).asText());
        JsonNode optionalSchema = body.get("tools").get(0).get("functionDeclarations").get(1).get("parametersJsonSchema");
        assertFalse(optionalSchema.has("required"));
        JsonNode functionResponse = body.get("contents").get(1).get("parts").get(0).get("functionResponse");
        assertEquals("user", body.get("contents").get(1).get("role").asText());
        assertEquals("search_support_knowledge", functionResponse.get("name").asText());
        assertTrue(functionResponse.get("response").get("results").isArray());
        assertFalse(body.get("generationConfig").has("responseMimeType"));
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
