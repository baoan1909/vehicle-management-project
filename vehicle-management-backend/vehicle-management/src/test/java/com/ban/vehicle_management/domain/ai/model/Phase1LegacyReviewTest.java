package com.ban.vehicle_management.domain.ai.model;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
class Phase1LegacyReviewTest {
    static Stream<org.junit.jupiter.params.provider.Arguments> reviewedCases() throws Exception {
        var root = new ObjectMapper().readTree(Path.of("docs/ai-knowledge/phase-0/authoring/evaluation/legacy-120-review.json").toFile());
        var cases = new java.util.ArrayList<org.junit.jupiter.params.provider.Arguments>();
        for (var row : root) if (row.path("reviewBatch").asInt() == 1 && !row.path("expectedRuntimeIntent").isNull()) {
            cases.add(org.junit.jupiter.params.provider.Arguments.of(row.path("caseId").asText(), row.path("question").asText(), row.path("expectedRuntimeIntent").asText()));
        }
        assertEquals(29, cases.size());
        return cases.stream();
    }
    @ParameterizedTest(name="{0}") @MethodSource("reviewedCases")
    void reviewedLegacyBehavior(String id, String question, String expected) {
        assertEquals(expected, AssistantIntentRouter.route(question).intent().name(), id+": "+question);
    }
}
