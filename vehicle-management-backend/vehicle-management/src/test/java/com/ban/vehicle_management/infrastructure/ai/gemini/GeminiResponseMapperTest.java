package com.ban.vehicle_management.infrastructure.ai.gemini;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertEquals(429, response.providerErrorDetail().providerStatus());
        assertTrue(response.providerErrorDetail().retryable());
    }

    @Test
    void shouldParseInvalidArgumentAsNonRetryableWithFieldViolations() {
        String body = """
                {
                  "error": {
                    "code": 400,
                    "status": "INVALID_ARGUMENT",
                    "message": "Invalid JSON payload received. apiKey=secret-token should be hidden",
                    "details": [{
                      "fieldViolations": [{
                        "field": "tools[0].function_declarations[0].parameters",
                        "description": "Unknown name additionalProperties"
                      }]
                    }]
                  }
                }
                """;

        AiResponse response = mapper.toAiResponse(400, body);

        assertEquals("HTTP_400", response.failureCode());
        assertFalse(response.retryable());
        assertEquals(400, response.providerErrorDetail().providerStatus());
        assertEquals("INVALID_ARGUMENT", response.providerErrorDetail().providerErrorCode());
        assertTrue(response.providerErrorDetail().providerErrorMessageRedacted().contains("[REDACTED]"));
        assertFalse(response.providerErrorDetail().providerErrorMessageRedacted().contains("secret-token"));
        assertEquals("[{\"field\":\"tools[0].function_declarations[0].parameters\",\"description\":\"Unknown name additionalProperties\"}]",
                response.providerErrorDetail().fieldViolationsRedacted());
        assertFalse(response.providerErrorDetail().retryable());
    }

    @Test
    void shouldTreatServerErrorsAsRetryable() {
        AiResponse response = mapper.toAiResponse(503, "{\"error\":{\"code\":503,\"status\":\"UNAVAILABLE\",\"message\":\"try later\"}}");

        assertEquals("HTTP_5XX", response.failureCode());
        assertTrue(response.retryable());
        assertEquals("UNAVAILABLE", response.providerErrorDetail().providerErrorCode());
    }

    @Test
    void shouldMapFunctionCall() {
        String body = """
                {
                  "candidates": [{
                    "finishReason": "STOP",
                    "content": {
                      "parts": [{
                        "functionCall": {
                          "name": "search_support_knowledge",
                          "args": {"query": "ve thang"}
                        }
                      }]
                    }
                  }],
                  "usageMetadata": {"promptTokenCount": 10, "candidatesTokenCount": 2}
                }
                """;

        AiResponse response = mapper.toAiResponse(200, body);

        assertTrue(response.success());
        assertEquals("search_support_knowledge", response.functionCall().name());
        assertEquals("{\"query\":\"ve thang\"}", response.functionCall().argumentsJson());
        assertEquals("STOP", response.finishReason());
    }
}
