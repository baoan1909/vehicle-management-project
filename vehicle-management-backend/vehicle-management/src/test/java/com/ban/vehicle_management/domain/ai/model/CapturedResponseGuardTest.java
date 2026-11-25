package com.ban.vehicle_management.domain.ai.model;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Replay real provider wording through the current success-claim guard without another API call. */
class CapturedResponseGuardTest {
    private static final Path EVIDENCE = Path.of(
            "docs/ai-knowledge/phase-0/authoring/evaluation/evidence/batch-03");

    static Stream<Arguments> capturedResponses() throws Exception {
        ObjectMapper json = new ObjectMapper();
        List<Arguments> cases = new ArrayList<>();
        for (String file : List.of("release-candidate-runtime.json", "repeat-2.json", "repeat-3.json")) {
            JsonNode records = json.readTree(Files.readString(EVIDENCE.resolve(file)));
            for (JsonNode row : records) {
                int attempt = 0;
                for (JsonNode call : row.path("attempts")) {
                    attempt++;
                    if (!call.path("success").asBoolean() || call.path("rawResponse").isNull()) continue;
                    String text = json.readTree(call.path("rawResponse").asText()).path("responseText").asText();
                    cases.add(Arguments.of(file + ":" + row.path("caseId").asText() + ":" + attempt,
                            row.path("question").asText(), text));
                }
            }
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("capturedResponses")
    void groundedGuidanceNeverBecomesAnUnverifiedCompletedAction(String caseId, String question, String text) {
        var envelope = new AssistantResponseEnvelope(text, List.of(), List.of(), 1.0, false);
        var checked = AssistantOutputValidator.validate(envelope,
                new AssistantOutputValidator.ValidationContext(Set.of(), Set.of(), question,
                        List.of(), List.of(), false, false));
        assertFalse(checked.violations().contains("UNVERIFIED_ACTION_SUCCESS_CLAIM"), caseId);
    }
}
