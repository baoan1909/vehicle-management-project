package com.ban.vehicle_management.domain.ai.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class Phase1RoutingEvaluationTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final List<Map<String, Object>> RESULTS = new ArrayList<>();

    static Stream<JsonNode> questions() throws Exception {
        Path path = Path.of("docs/ai-knowledge/phase-0/authoring/evaluation/batch-01-questions.jsonl");
        List<JsonNode> rows = new ArrayList<>();
        for (String line : Files.readAllLines(path)) if (!line.isBlank()) rows.add(MAPPER.readTree(line));
        assertEquals(240, rows.size(), "Batch one coverage must contain 40 families x six variants");
        return rows.stream();
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("questions")
    void evaluatesScenario(JsonNode row) {
        List<String> context = new ArrayList<>();
        row.path("context").forEach(value -> context.add(value.asText()));
        var resolved = AssistantQuestionResolver.resolve(row.path("question").asText(), context);
        String expected = row.path("expectedIntent").asText();
        String actual = resolved.decision().intent().name();
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : List.of("caseId", "scenarioId", "question", "documentSlug", "variantType", "split")) {
            result.put(key, row.path(key).asText());
        }
        result.put("expected", expected);
        result.put("actual", actual);
        result.put("reason", resolved.decision().reason());
        result.put("query", resolved.query());
        result.put("passed", expected.equals(actual));
        RESULTS.add(result);
        assertEquals(expected, actual, row.path("caseId").asText() + ": " + row.path("question").asText());
    }

    @AfterAll
    static void saveEvidence() throws Exception {
        Path directory = Path.of("target/chatbox-phase1");
        Files.createDirectories(directory);
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(directory.resolve("routing-results.json").toFile(), RESULTS);
    }
}
