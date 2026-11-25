package com.ban.vehicle_management.ai.golden;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.application.ai.service.KnowledgeDocumentChunker;
import com.ban.vehicle_management.domain.ai.model.AssistantOutputValidator;
import com.ban.vehicle_management.domain.ai.model.AssistantResponseEnvelope;
import com.ban.vehicle_management.domain.ai.model.GroundedFaqLookup;
import com.ban.vehicle_management.domain.ai.model.KnowledgeSearchResult;
import com.ban.vehicle_management.infrastructure.ai.extraction.MarkdownDocumentExtractor;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Reviewed DRAFT content fixtures, without DB publication, provider HTTP or model acceptance. */
class ReviewedFaqRegressionTest {
    private static final Path KNOWLEDGE = Path.of("docs/ai-knowledge/phase-0/authoring/knowledge");

    @ParameterizedTest
    @ValueSource(strings = {
            "Vì sao chưa hoàn tất phiếu báo mất thẻ được?",
            "Vi sao chua hoan tat phieu bao mat the duoc?"
    })
    void publicAndCustomerGuidanceAgreeWithoutRemovingCompletionConditions(String question) throws Exception {
        List<KnowledgeSearchResult> sources = new ArrayList<>(faqSources("common-errors-and-troubleshooting"));
        sources.addAll(faqSources("cards-and-lost-card"));

        var lookup = GroundedFaqLookup.findCommon(question, sources);

        assertFalse(lookup.conflict(), "Equivalent reviewed answers must not conflict across allowed scopes");
        assertNotNull(lookup.match());
        String answer = lookup.match().answer();
        assertTrue(answer.contains("thanh toán đã được ghi nhận đầy đủ"));
        assertTrue(answer.contains("Nếu cần thay thẻ"));
        assertTrue(answer.contains("đúng loại và cùng bãi"));
        assertTrue(answer.contains("không") || answer.contains("Không"));
        assertTrue(validate(lookup.match().answer(), sources, lookup.match().chunkId()).valid());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Hủy đặt chỗ đã thanh toán có được hoàn tiền không?",
            "Huy dat cho da thanh toan co duoc hoan tien khong?"
    })
    void cancellationPolicyIsGuidanceAndNeverAnAcknowledgementOfCancellation(String question) throws Exception {
        List<KnowledgeSearchResult> sources = faqSources("reservations-and-holds");

        var lookup = GroundedFaqLookup.findCommon(question, sources);

        assertFalse(lookup.conflict());
        assertNotNull(lookup.match());
        String answer = lookup.match().answer();
        assertTrue(answer.contains("không được hoàn tiền"));
        assertTrue(answer.contains("còn được giữ và chưa sử dụng"));
        assertTrue(validate(answer, sources, lookup.match().chunkId()).valid());
        assertFalse(validate(answer + " Tôi đã hủy đặt chỗ cho bạn.", sources,
                lookup.match().chunkId()).valid(), "A real success claim must remain blocked");
    }

    @Test
    void aFollowUpCannotReuseTheInitialProcedureWithoutKnowingTheFailedStep() throws Exception {
        var lookup = GroundedFaqLookup.findCommon("Đã làm rồi vẫn lỗi", faqSources("authentication-and-profile"));
        assertFalse(lookup.conflict());
        assertTrue(lookup.match() == null);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Đăng ký vé tháng như thế nào?", "dang ky ve thang nhu the nao",
            "Đăng ký vé tháng cần làm gì?", "cach dang ky ve thang", "dk ve thang nhu nao"
    })
    void registrationGuidanceIncludesTheActualDateWindowWithoutPromisingActivation(String question) throws Exception {
        var sources = faqSources("subscription-lifecycle");
        var lookup = GroundedFaqLookup.findCommon(question, sources);
        assertNotNull(lookup.match());
        var answer = lookup.match().answer();
        assertTrue(answer.contains("từ hôm nay đến 7 ngày tới theo ngày tại Việt Nam"));
        assertTrue(answer.contains("theo dõi duyệt"));
        assertTrue(answer.contains("thanh toán xong chưa tự chứng minh vé đã dùng được"));
        assertTrue(validate(answer, sources, lookup.match().chunkId()).valid());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Nhân viên tạo phiếu mất thẻ không được thì làm sao?",
            "cach kiem tra khi tao phieu mat the that bai",
            "Phiếu báo mất thẻ bị lỗi cần làm gì?"
    })
    void lostCardTroubleshootingKeepsTheDuplicateAndSupportTicketBoundaries(String question) throws Exception {
        var sources = faqSources("cards-and-lost-card");
        var lookup = GroundedFaqLookup.findCommon(question, sources);
        assertNotNull(lookup.match());
        var answer = lookup.match().answer();
        assertTrue(answer.contains("phiếu đang mở"));
        assertTrue(answer.contains("Phiếu hỗ trợ báo lỗi phần mềm không thay thế phiếu báo mất thẻ"));
        assertTrue(answer.contains("bước hoàn tất"));
        assertTrue(validate(answer, sources, lookup.match().chunkId()).valid());
    }

    private AssistantOutputValidator.ValidationResult validate(
            String answer, List<KnowledgeSearchResult> sources, UUID chunkId) {
        var source = sources.stream().filter(item -> item.chunkId().equals(chunkId)).findFirst().orElseThrow();
        var envelope = new AssistantResponseEnvelope(answer, List.of(
                new AssistantResponseEnvelope.AssistantCitation("C1", source.chunkId(), source.documentId(),
                        source.title(), source.sourcePage(), source.sourceSection())), List.of(), 1.0, false);
        return AssistantOutputValidator.validate(envelope, new AssistantOutputValidator.ValidationContext(
                Set.of("C1"), Set.of(chunkId), "Hướng dẫn quy trình", List.of(source.content()),
                List.of(), false, true));
    }

    private List<KnowledgeSearchResult> faqSources(String slug) throws Exception {
        UUID documentId = UUID.nameUUIDFromBytes(slug.getBytes(StandardCharsets.UTF_8));
        var extracted = new MarkdownDocumentExtractor().extract(
                Files.readAllBytes(KNOWLEDGE.resolve(slug + ".md")), "md", "text/markdown");
        var chunker = new KnowledgeDocumentChunker();
        var blocks = chunker.createBlocks(documentId, 1, extracted);
        List<KnowledgeSearchResult> sources = new ArrayList<>();
        for (var chunk : chunker.createChunks(documentId, 1, slug, blocks)) {
            var faq = blocks.stream().filter(block -> block.getBlockIndex() >= chunk.getStartBlockIndex()
                    && block.getBlockIndex() <= chunk.getEndBlockIndex()
                    && GroundedFaqLookup.isFaqSection(block.getSourceSection())).toList();
            if (faq.isEmpty()) continue;
            var first = faq.getFirst();
            UUID chunkId = UUID.nameUUIDFromBytes((slug + "/" + chunk.getChunkIndex()).getBytes(StandardCharsets.UTF_8));
            sources.add(new KnowledgeSearchResult(documentId, chunkId, slug,
                    String.join("\n", faq.stream().map(block -> block.getContent()).toList()), null,
                    first.getSourcePage(), first.getSourceSection(), BigDecimal.ONE));
        }
        return sources;
    }
}
