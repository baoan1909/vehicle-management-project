package com.ban.vehicle_management.ai.golden;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class QwenApiCaseSelectionTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test void everyRouteAndContextRemainEligibleForHttpEvaluation() {
        var rows = List.of(row("a", "batch-01", "PERSONAL_DATA"),
                row("b", "baseline", "PRIVACY_CROSS_ACCOUNT"), row("c", "new", "WRITE_ACTION"));
        ((com.fasterxml.jackson.databind.node.ObjectNode) rows.getLast()).putArray("context").add("Không tạo phiếu");
        assertEquals(rows, QwenApiCaseSelection.select(rows, "all", 0, 6));
        assertEquals(List.of(rows.get(1)), QwenApiCaseSelection.select(rows, "baseline", 0, 6));
    }

    @Test void shardsHaveStableOrderAndDoNotRepeatCases() {
        var rows = List.of(row("a", "new", "STATIC_KNOWLEDGE"), row("b", "new", "STATIC_KNOWLEDGE"),
                row("c", "new", "CLARIFICATION"));
        assertEquals(rows.subList(0, 2), QwenApiCaseSelection.select(rows, "new", 0, 2));
        assertEquals(rows.subList(2, 3), QwenApiCaseSelection.select(rows, "new", 2, 2));
    }

    @Test void duplicatesInvalidBoundsAndEmptySelectionAreErrors() {
        var a = row("a", "new", "PERSONAL_DATA");
        assertThrows(IllegalArgumentException.class, () -> QwenApiCaseSelection.select(List.of(a, a), "all", 0, 1));
        for (int limit : List.of(0, 201)) assertThrows(IllegalArgumentException.class,
                () -> QwenApiCaseSelection.select(List.of(a), "all", 0, limit));
        assertThrows(IllegalArgumentException.class, () -> QwenApiCaseSelection.select(List.of(a), "all", -1, 1));
        assertThrows(IllegalArgumentException.class, () -> QwenApiCaseSelection.select(List.of(a), "new", 1, 1));
        assertThrows(IllegalArgumentException.class, () -> QwenApiCaseSelection.select(List.of(a), "missing", 0, 1));
    }

    private JsonNode row(String id, String dataset, String intent) {
        return json.createObjectNode().put("caseId", id).put("evaluationDataset", dataset).put("expectedIntent", intent);
    }
}
