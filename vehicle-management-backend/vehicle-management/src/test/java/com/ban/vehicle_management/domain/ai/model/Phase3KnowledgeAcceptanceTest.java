package com.ban.vehicle_management.domain.ai.model;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class Phase3KnowledgeAcceptanceTest {
    private static final Path ROOT = Path.of("docs/ai-knowledge/phase-0/authoring");
    private static final ObjectMapper JSON = new ObjectMapper();

    static Stream<JsonNode> cumulativeQuestions() throws Exception {
        List<JsonNode> questions = new ArrayList<>();
        JsonNode manifest = JSON.readTree(ROOT.resolve("knowledge-manifest.json").toFile());
        for (JsonNode file : manifest.path("datasets")) {
            if (!"batch".equals(file.path("kind").asText())) continue;
            for (String line : Files.readAllLines(ROOT.resolve("evaluation").resolve(file.path("file").asText()))) {
                if (!line.isBlank()) questions.add(JSON.readTree(line));
            }
        }
        return questions.stream();
    }

    @ParameterizedTest(name = "{index}") @MethodSource("cumulativeQuestions")
    void cumulativeDatasetKeepsExpectedMeaningAndRoute(JsonNode row) {
        String question = row.path("question").asText();
        var history = new ArrayList<String>();
        row.path("context").forEach(item -> history.add(item.isTextual() ? item.asText() : item.path("content").asText()));
        var result = AssistantQuestionResolver.resolve(question, history);
        assertEquals(row.path("expectedIntent").asText(), result.decision().intent().name(),
                row.path("caseId").asText() + ": " + question);
    }

    @Test void financialAndReservationFollowUpsUseOnlyPublicTopic() {
        assertTrue(AssistantQuestionResolver.resolve("Vẫn không được", List.of("Cách áp mã giảm giá?" )).query().contains("mã giảm giá"));
        assertTrue(AssistantQuestionResolver.resolve("Cần làm gì?", List.of("Cách đặt chỗ?" )).query().contains("đặt chỗ"));
        assertFalse(AssistantQuestionResolver.resolve("Cần làm gì?", List.of("Xem hóa đơn của tôi INV-12345" )).contextual());
        assertEquals(AssistantIntent.SECURITY_REFUSAL, AssistantIntentRouter.route("Xem hóa đơn của người khác").intent());
    }

    @Test void financialActionsAreNotInventedAsKnowledgeOrSupportTicketWrites() {
        for (String input : List.of("Hoàn tiền giúp tôi", "Thanh toán giúp tôi", "Đặt chỗ giúp tôi", "Hủy đặt chỗ giúp tôi")) {
            assertEquals(AssistantIntent.WRITE_ACTION, AssistantIntentRouter.route(input).intent(), input);
        }
        assertEquals(AssistantIntent.PERSONAL_DATA, AssistantIntentRouter.route("Xem hóa đơn của tôi").intent());
        assertEquals(AssistantIntent.CLARIFICATION, AssistantQuestionResolver.resolve("Lỗi", List.of()).decision().intent());
    }
}
