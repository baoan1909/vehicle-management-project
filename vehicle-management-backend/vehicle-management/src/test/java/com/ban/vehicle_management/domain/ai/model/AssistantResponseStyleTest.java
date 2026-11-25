package com.ban.vehicle_management.domain.ai.model;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class AssistantResponseStyleTest {
    @Test
    void formatsOnlyConsecutiveInlineStepsWithoutChangingAnyNonWhitespaceCharacter() {
        String input = "Làm như sau: 1. Mở Vé tháng của tôi. 2. Kiểm tra 80.000 đ, ngày 19/11/2026. 3. Không gửi lại nếu chưa rõ kết quả.";
        String display = AssistantResponseStyle.formatForDisplay(input);
        assertEquals("Làm như sau:\n1. Mở Vé tháng của tôi.\n2. Kiểm tra 80.000 đ, ngày 19/11/2026.\n3. Không gửi lại nếu chưa rõ kết quả.", display);
        assertEquals(input.replaceAll("\\s", ""), display.replaceAll("\\s", ""));
        assertEquals(display, AssistantResponseStyle.formatForDisplay(display));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Phí 1.000 đ đến 2.000 đ, ngày 1.2.2026.", "Chọn 2. Mở trang rồi 4. Lưu.",
            "Chỉ có 1. Mở trang.", "Không có các bước.", "Đã có dòng:\n1. Mở trang.\n2. Lưu."})
    void leavesAmountsDatesNonSequentialNumbersAndExistingParagraphsIntact(String text) {
        assertEquals(text, AssistantResponseStyle.formatForDisplay(text));
    }
    @ParameterizedTest
    @ValueSource(strings = {"Vé đang PENDING_SPACE", "subscriptionId: nội bộ", "Quyền SUBSCRIPTION_READ_OWN",
            "Gọi /api/parking/parking-sessions/me", "ID 00000000-0000-4000-8000-000000000001"})
    void rejectsInternalDetailsForOrdinaryUser(String answer) {
        assertTrue(AssistantResponseStyle.containsUnnecessaryInternalDetail(answer, "Vé tháng như thế nào?"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Vé đang chờ chọn vị trí.", "Loại xe: Xe máy.", "Mã phiếu HT-12345.",
            "Hãy mở Lịch sử gửi xe và chọn Xem chi tiết.", "Tài khoản chưa được phép thực hiện thao tác này."})
    void keepsOrdinaryProductTermsAndVisibleCodes(String answer) {
        assertFalse(AssistantResponseStyle.containsUnnecessaryInternalDetail(answer, "Hướng dẫn sử dụng"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Giải thích enum PENDING_SPACE", "API này dùng type gì?"})
    void technicalExplanationStillUsesIndependentSecurityValidation(String question) {
        assertFalse(AssistantResponseStyle.containsUnnecessaryInternalDetail("PENDING_SPACE là chờ chọn vị trí.", question));
    }
}
