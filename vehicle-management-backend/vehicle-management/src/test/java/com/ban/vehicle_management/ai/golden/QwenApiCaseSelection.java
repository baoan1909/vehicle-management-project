package com.ban.vehicle_management.ai.golden;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;
import java.util.List;

/** Select HTTP evaluation shards without filtering out hard routes or language variants. */
final class QwenApiCaseSelection {
    private QwenApiCaseSelection() { }

    static List<JsonNode> select(List<JsonNode> rows, String dataset, int offset, int limit) {
        if (offset < 0 || limit < 1 || limit > 200 || dataset == null || dataset.isBlank()) {
            throw new IllegalArgumentException("Require dataset, offset>=0 and limit1..200");
        }
        var ids = new HashSet<String>();
        for (var row : rows) {
            String id = row.path("caseId").asText();
            if (id.isBlank() || !ids.add(id)) throw new IllegalArgumentException("Missing/duplicate case ID");
        }
        var filtered = rows.stream().filter(row -> "all".equals(dataset)
                || dataset.equals(row.path("evaluationDataset").asText())).toList();
        if (offset >= filtered.size()) throw new IllegalArgumentException("Empty HTTP evaluation shard");
        return List.copyOf(filtered.subList(offset, Math.min(filtered.size(), offset + limit)));
    }
}
