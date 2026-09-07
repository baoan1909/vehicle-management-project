package com.ban.vehicle_management.infrastructure.ai.gemini;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.domain.ai.model.AiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class GeminiResponseMapperTest {

    private final GeminiResponseMapper mapper = new GeminiResponseMapper(new ObjectMapper());

    @Test
    void shouldMapGenerateContentTextAndTokens() {
        String body = """
                {
                  "candidates": [{"content": {"parts": [{"text": "{\\"responseText\\":\\"Xin chao\\"}"}]}}],
                  "usageMetadata": {"promptTokenCount": 12, "candidatesTokenCount": 5}
                }
                """;

        AiResponse response = mapper.toAiResponse(200, body);

        assertTrue(response.success());
        assertEquals("{\"responseText\":\"Xin chao\"}", response.text());
        assertEquals(12, response.inputTokens());
        assertEquals(5, response.outputTokens());
    }

    @Test
    void shouldTreatRateLimitAsRetryable() {
        AiResponse response = mapper.toAiResponse(429, "{}");

        assertEquals("HTTP_429", response.failureCode());
        assertTrue(response.retryable());
    }
}
