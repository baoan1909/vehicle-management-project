package com.ban.vehicle_management.ai.golden;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bounded, deterministic live shards. This is sampling, not full-corpus acceptance. */
final class QwenLiveCaseSelection {
    private QwenLiveCaseSelection() { }

    record Selection(int totalEligible, List<JsonNode> cases) { }

    static Selection select(List<JsonNode> rows, Map<String, Integer> batches, int batch, int offset, int limit) {
        if (batch < 0 || batch > 4 || offset < 0 || limit < 1 || limit > 50) {
            throw new IllegalArgumentException("Require batch0..4, nonnegative offset and limit1..50");
        }
        Map<String, JsonNode> eligible = new LinkedHashMap<>();
        for (JsonNode row : rows) {
            String slug = row.path("documentSlug").asText();
            if (!"STATIC_KNOWLEDGE".equals(row.path("expectedIntent").asText())
                    || !row.path("context").isEmpty()
                    || (batch != 0 && !Integer.valueOf(batch).equals(batches.get(slug)))) continue;
            eligible.putIfAbsent(slug + "/" + row.path("variantType").asText(), row);
        }
        List<JsonNode> candidates = List.copyOf(eligible.values());
        if (offset >= candidates.size()) {
            throw new IllegalArgumentException("Requested live shard is empty");
        }
        return new Selection(candidates.size(), List.copyOf(candidates.subList(offset,
                Math.min(candidates.size(), offset + limit))));
    }
}
