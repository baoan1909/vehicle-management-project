package com.ban.vehicle_management.ai.golden;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.ban.vehicle_management.application.ai.port.out.KnowledgeIndexVersionPortOut;
import com.ban.vehicle_management.application.ai.service.EmbeddingProperties;
import com.ban.vehicle_management.application.ai.service.KnowledgeRetrievalService;
import com.ban.vehicle_management.domain.ai.model.KnowledgeRetrievalResult;
import com.ban.vehicle_management.domain.ai.model.KnowledgeRetrievalContext;
import com.ban.vehicle_management.domain.ai.model.KnowledgeSearchResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Live golden evaluation against the real hybrid retrieval stack. Skipped with
 * a recorded reason when no embedding provider or active index is available;
 * the Recall@5 ≥ 90% gate can only be measured in an environment with a live
 * corpus, provider key and activated index.
 */
@SpringBootTest
@ActiveProfiles("test")
class GoldenRetrievalLiveIT {

    @Autowired
    private KnowledgeRetrievalService retrievalService;

    @Autowired
    private EmbeddingProperties embeddingProperties;

    @Autowired
    private KnowledgeIndexVersionPortOut indexVersionPortOut;

    @Test
    @Transactional(readOnly = true)
    void goldenRecallAt5MeetsGateOnLiveCorpus() throws Exception {
        assumeTrue(embeddingProperties.isEnabled(), "Embedding provider disabled; live golden gate not measurable");
        assumeTrue(indexVersionPortOut.findActive().isPresent(), "No active index; live golden gate not measurable");

        List<GoldenEvaluationRunner.GoldenQuestion> questions = loadGoldenQuestions();
        GoldenEvaluationRunner.EvaluationReport report = GoldenEvaluationRunner.evaluate(
                questions,
                question -> {
                    KnowledgeRetrievalResult result = retrievalService.search(
                            KnowledgeRetrievalContext.global(), question.question(), List.of("PUBLIC"), 5);
                    return new GoldenEvaluationRunner.RetrievalRun(
                            result.results().stream().map(KnowledgeSearchResult::title).toList(),
                            List.of("PUBLIC"),
                            new ArrayList<>(result.citationLabels().values()));
                });

        System.out.println("GOLDEN LIVE: recall@5=" + report.recallAt5()
                + " mrr=" + report.mrr()
                + " accentEquivalence=" + report.accentEquivalence()
                + " citationCorrectness=" + report.citationCorrectness()
                + " scopeLeakage=" + report.scopeLeakage());
        org.junit.jupiter.api.Assertions.assertTrue(report.recallAt5() >= 0.90,
                "Recall@5 gate: " + report.recallAt5());
        org.junit.jupiter.api.Assertions.assertEquals(0.0, report.scopeLeakage());
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
