package com.ban.vehicle_management.domain.ai.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AiToolRegistryTest {

    private final AiToolRegistry registry = new AiToolRegistry();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldExposeJsonSchemaWithRequiredFieldsOnlyForMandatoryArguments() throws Exception {
        JsonNode searchSchema = schema("search_support_knowledge");
        JsonNode listSchema = schema("list_my_support_tickets");
        JsonNode createSchema = schema("create_support_ticket");

        assertFalse(searchSchema.get("additionalProperties").asBoolean());
        assertEquals("query", searchSchema.get("required").get(0).asText());

        assertFalse(listSchema.get("additionalProperties").asBoolean());
        assertFalse(listSchema.has("required"));

        assertFalse(createSchema.get("additionalProperties").asBoolean());
        assertEquals(3, createSchema.get("required").size());
        assertEquals("categoryId", createSchema.get("required").get(0).asText());
        assertEquals("title", createSchema.get("required").get(1).asText());
        assertEquals("content", createSchema.get("required").get(2).asText());
    }

    private JsonNode schema(String toolName) throws Exception {
        return objectMapper.readTree(registry.require(toolName).declaration().parametersSchema());
    }
}
