package com.ban.vehicle_management.domain.ai.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GroundedContextPackTest {

    @Test
    void buildShouldRespectBudgetAndLabelChunks() {
        List<KnowledgeSearchResult> results = List.of(
                result("Quy trình đăng ký", "Nội dung quy trình đăng ký thẻ xe ".repeat(50)),
                result("Biểu phí", "Biểu phí gửi xe máy ".repeat(50)),
                result("Thủ tục", "Thủ tục cấp lại thẻ ".repeat(50)));
        GroundedContextPack.ContextPack pack =
                GroundedContextPack.build("Làm thẻ xe thế nào?", results, 1200, 5, 1500);
        assertTrue(pack.estimatedTokens() <= 1200);
        assertTrue(pack.chunks().size() >= 1 && pack.chunks().size() <= 3);
        assertTrue(pack.text().contains("[C1]"));
        assertTrue(pack.text().contains("KHÔNG ĐÁNG TIN CẬY"));
    }

    @Test
    void buildShouldDedupeByChunkId() {
        KnowledgeSearchResult first = result("A", "Nội dung A");
        GroundedContextPack.ContextPack pack = GroundedContextPack.build(
                "Hỏi", List.of(first, first, result("B", "Nội dung B")), 3000, 5, 1500);
        assertTrue(pack.chunks().size() == 2);
    }

    private KnowledgeSearchResult result(String title, String content) {
        return new KnowledgeSearchResult(
                UUID.randomUUID(), UUID.randomUUID(), title, content, null, 1, "Mục 1", BigDecimal.ONE);
    }
}
