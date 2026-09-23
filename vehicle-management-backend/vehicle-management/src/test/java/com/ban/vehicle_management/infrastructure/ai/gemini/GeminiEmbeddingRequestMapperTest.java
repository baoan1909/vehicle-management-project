package com.ban.vehicle_management.infrastructure.ai.gemini;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.EmbeddingRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class GeminiEmbeddingRequestMapperTest {

    private final GeminiEmbeddingRequestMapper mapper = new GeminiEmbeddingRequestMapper(new ObjectMapper());

    @Test
    void shouldBuildEmbedContentBodyWithDimensionality() {
        AiModelConfiguration configuration = new AiModelConfiguration();
        configuration.setOutputDimension(768);
        String body = mapper.toEmbedContentBody(new EmbeddingRequest("title: A | text: B", null), configuration);

        assertTrue(body.contains("\"content\""));
        assertTrue(body.contains("title: A"));
        assertTrue(body.contains("\"output_dimensionality\":768"));
    }

    @Test
    void shouldRejectMissingDimensionality() {
        AiModelConfiguration configuration = new AiModelConfiguration();

        assertThrows(
                IllegalArgumentException.class,
                () -> mapper.toEmbedContentBody(new EmbeddingRequest("hello", null), configuration)
        );
    }
}
