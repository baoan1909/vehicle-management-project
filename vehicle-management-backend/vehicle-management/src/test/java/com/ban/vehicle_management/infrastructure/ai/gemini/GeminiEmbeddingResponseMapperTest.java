package com.ban.vehicle_management.infrastructure.ai.gemini;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.domain.ai.model.EmbeddingResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class GeminiEmbeddingResponseMapperTest {

    private final GeminiEmbeddingResponseMapper mapper = new GeminiEmbeddingResponseMapper(new ObjectMapper());

    @Test
    void shouldParseSuccessfulEmbedding() {
        String body = "{\"embedding\":{\"values\":[0.1,0.2,0.3]}}";
        EmbeddingResult result = mapper.toEmbeddingResult(200, body, null, 3);

        assertTrue(result.isSuccess());
        assertEquals(3, result.getVector().dimension());
        assertEquals(0.2, result.getVector().values()[1]);
    }

    @Test
    void shouldRejectDimensionMismatch() {
        String body = "{\"embedding\":{\"values\":[0.1,0.2,0.3]}}";
        EmbeddingResult result = mapper.toEmbeddingResult(200, body, null, 768);

        assertFalse(result.isSuccess());
        assertEquals("EMBEDDING_DIMENSION_MISMATCH", result.getFailureCode());
        assertFalse(result.isRetryable());
    }

    @Test
    void shouldRejectMissingEmbedding() {
        String body = "{\"foo\":\"bar\"}";
        EmbeddingResult result = mapper.toEmbeddingResult(200, body, null, 3);

        assertFalse(result.isSuccess());
        assertEquals("EMBEDDING_INVALID_RESPONSE", result.getFailureCode());
        assertTrue(result.isRetryable());
    }

    @Test
    void shouldRejectNanVector() {
        String body = "{\"embedding\":{\"values\":[0.1,0.2,null]}}";
        EmbeddingResult result = mapper.toEmbeddingResult(200, body, null, 3);

        assertFalse(result.isSuccess());
        assertEquals("EMBEDDING_VECTOR_INVALID", result.getFailureCode());
        assertFalse(result.isRetryable());
    }

    @Test
    void shouldClassifyRateLimitAsRetryable() {
        String body = "{\"error\":{\"code\":429,\"status\":\"RESOURCE_EXHAUSTED\",\"message\":\"Rate limit\"}}";
        EmbeddingResult result = mapper.toEmbeddingResult(429, body, 30L, 3);

        assertFalse(result.isSuccess());
        assertEquals("EMBEDDING_RATE_LIMITED", result.getFailureCode());
        assertTrue(result.isRetryable());
        assertEquals(30L, result.getFailure().retryAfterSeconds());
    }

    @Test
    void shouldClassifyBadRequestAsPermanent() {
        String body = "{\"error\":{\"code\":400,\"status\":\"INVALID_ARGUMENT\",\"message\":\"Bad text\"}}";
        EmbeddingResult result = mapper.toEmbeddingResult(400, body, null, 3);

        assertFalse(result.isSuccess());
        assertEquals("EMBEDDING_INVALID_REQUEST", result.getFailureCode());
        assertFalse(result.isRetryable());
        assertEquals("INVALID_ARGUMENT", result.getFailure().providerErrorCode());
    }
}