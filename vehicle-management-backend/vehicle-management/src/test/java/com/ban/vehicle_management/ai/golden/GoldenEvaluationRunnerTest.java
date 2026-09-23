package com.ban.vehicle_management.ai.golden;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GoldenEvaluationRunnerTest {

    @Test
    void evaluateShouldScorePerfectRetrieverAtFullRecall() throws Exception {
        List<GoldenEvaluationRunner.GoldenQuestion> questions = loadGoldenQuestions();
        assertEquals(12, questions.size());

        GoldenEvaluationRunner.EvaluationReport report = GoldenEvaluationRunner.evaluate(
                questions,
                question -> new GoldenEvaluationRunner.RetrievalRun(
                        List.of("Quy trình " + question.expectedKeywords().get(0) + " CoParking",
                                "Tài liệu khác"),
                        List.of("PUBLIC", "PUBLIC"),
                        List.of("C1")));

        assertEquals(1.0, report.recallAt1());
        assertEquals(1.0, report.recallAt5());
        assertEquals(1.0, report.mrr());
        assertEquals(1.0, report.accentEquivalence());
        assertEquals(1.0, report.citationCorrectness());
        assertEquals(0.0, report.scopeLeakage());
    }

    @Test
    void evaluateShouldDetectMissesLeaksAndBadCitations() {
        List<GoldenEvaluationRunner.GoldenQuestion> questions = List.of(
                new GoldenEvaluationRunner.GoldenQuestion(
                        "T1", "Câu hỏi về thẻ xe?", "cau hoi ve the xe?",
                        List.of("zzz-không-tồn-tại"), "PUBLIC", 1),
                new GoldenEvaluationRunner.GoldenQuestion(
                        "T2", "Câu hỏi về thẻ xe?", "cau hoi ve the xe?",
                        List.of("zzz-không-tồn-tại"), "PUBLIC", 1));

        GoldenEvaluationRunner.EvaluationReport report = GoldenEvaluationRunner.evaluate(
                questions,
                question -> new GoldenEvaluationRunner.RetrievalRun(
                        List.of("Tài liệu ADMIN mật"),
                        List.of("ADMIN"),
                        List.of("C99", "bogus")));

        assertEquals(0.0, report.recallAt5());
        assertEquals(0.0, report.mrr());
        assertTrue(report.scopeLeakage() > 0);
        assertTrue(report.citationCorrectness() < 1.0);
    }

    private List<GoldenEvaluationRunner.GoldenQuestion> loadGoldenQuestions() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream stream = getClass().getResourceAsStream("/ai/golden/phase5-golden-questions.json")) {
            JsonNode root = mapper.readTree(stream);
            List<GoldenEvaluationRunner.GoldenQuestion> questions = new ArrayList<>();
            for (JsonNode node : root.get("questions")) {
                List<String> keywords = mapper.convertValue(
                        node.get("expectedKeywords"), new TypeReference<List<String>>() {
                        });
                questions.add(new GoldenEvaluationRunner.GoldenQuestion(
                        node.get("id").asText(),
                        node.get("question").asText(),
                        node.get("questionNoAccent").asText(),
                        keywords,
                        node.get("expectedScope").asText(),
                        node.get("minCitations").asInt()));
            }
            return questions;
        }
    }
}
