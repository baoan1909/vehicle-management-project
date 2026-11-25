package com.ban.vehicle_management.domain.ai.model;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AssistantOwnReadRouterTest {
    @Test
    void bothBatchOwnListVariantsResolveWithoutModelSelection() throws Exception {
        var mapper = new ObjectMapper();
        int checked = 0;
        for (String batch : new String[]{"01", "02"}) {
            Path file = Path.of("docs/ai-knowledge/phase-0/authoring/evaluation/batch-" + batch + "-questions.jsonl");
            for (String line : Files.readAllLines(file)) {
                var row = mapper.readTree(line);
                String id = row.path("caseId").asText();
                if (!id.matches("(?:SUP04|B2-VEH06|B2-SUB06|B2-HIST06)-[1-6]")) continue;
                assertEquals(row.path("expectedTool").asText(),
                        AssistantOwnReadRouter.resolve(row.path("question").asText()).orElse(""), id);
                checked++;
            }
        }
        assertEquals(24, checked);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Xem xe của người khác", "Xem xe của tôi biển số 59A12345",
        "Xem lịch sử gửi xe của tôi hôm qua", "Xem lịch sử gửi xe của tôi và xóa lượt cũ",
        "Xem phiếu của tôi đang xử lý", "Xem phiếu của tôi 6090c0ea-f5b9-4f40-89b1-db5f9b61b043",
        "Vé tháng của tôi ở bãi ABC", "Xem thẻ của tôi", "Làm sao thêm xe của tôi?",
        "Xem danh sách xe của tất cả khách hàng", "Xem hồ sơ của tôi"
    })
    void identifiersFiltersWritesAndUnsupportedResourcesNeverBecomeUnfilteredOwnLists(String question) {
        assertTrue(AssistantOwnReadRouter.resolve(question).isEmpty());
    }
}
