package com.ban.vehicle_management.ai.golden;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class QwenLiveCaseSelectionTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void shardsAreStableBoundedAndDisjointWithoutDroppingAvailableLanguageVariants() {
        List<JsonNode> rows = List.of(row("a", "ACCENTED"), row("a", "ACCENTED"),
                row("a", "UNACCENTED"), row("a", "PARAPHRASE"), row("b", "SHORT"));
        var first = QwenLiveCaseSelection.select(rows, Map.of("a", 1, "b", 2), 0, 0, 2);
        var next = QwenLiveCaseSelection.select(rows, Map.of("a", 1, "b", 2), 0, 2, 2);
        assertEquals(4, first.totalEligible());
        assertEquals(List.of("ACCENTED", "UNACCENTED"), first.cases().stream()
                .map(item -> item.path("variantType").asText()).toList());
        assertEquals(List.of("PARAPHRASE", "SHORT"), next.cases().stream()
                .map(item -> item.path("variantType").asText()).toList());
    }

    @Test
    void batchFilterKeepsOnlyRequestedDocumentsAndExcludesHistoryAndBusinessActions() {
        var contextual = row("b", "CONTEXTUAL");
        ((com.fasterxml.jackson.databind.node.ArrayNode) contextual.path("context")).add("Thông tin trước đó");
        var write = row("b", "TYPO");
        ((com.fasterxml.jackson.databind.node.ObjectNode) write).put("expectedIntent", "WRITE_ACTION");
        var selected = QwenLiveCaseSelection.select(List.of(row("a", "ACCENTED"),
                row("b", "SHORT"), contextual, write), Map.of("a", 1, "b", 2), 2, 0, 6);
        assertEquals(1, selected.totalEligible());
        assertEquals("b", selected.cases().getFirst().path("documentSlug").asText());
    }

    @Test
    void badLimitsAndEmptyShardsFailBeforeAnyProviderWork() {
        var rows = List.of(row("a", "ACCENTED"));
        for (int limit : List.of(0, 51)) assertThrows(IllegalArgumentException.class,
                () -> QwenLiveCaseSelection.select(rows, Map.of("a", 1), 1, 0, limit));
        assertThrows(IllegalArgumentException.class,
                () -> QwenLiveCaseSelection.select(rows, Map.of("a", 1), 1, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> QwenLiveCaseSelection.select(rows, Map.of("a", 1), 5, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> QwenLiveCaseSelection.select(rows, Map.of("a", 1), 1, -1, 1));
    }

    private JsonNode row(String slug, String variant) {
        var row = json.createObjectNode().put("documentSlug", slug)
                .put("variantType", variant).put("expectedIntent", "STATIC_KNOWLEDGE");
        row.putArray("context");
        return row;
    }
}
