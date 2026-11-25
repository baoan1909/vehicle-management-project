package com.ban.vehicle_management.domain.operations.chatmessage.policy;

import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessage;
import com.ban.vehicle_management.shared.enumeration.operations.ChatMessageType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChatMessagePolicyTest {

    private final ChatMessagePolicy policy = new ChatMessagePolicy();

    @Test
    void initializeTextNormalizesContent() {
        ChatMessage message = new ChatMessage();
        message.setContent("  Xin chao  ");

        policy.initializeText(message);

        assertEquals(ChatMessageType.TEXT, message.getMessageType());
        assertEquals("Xin chao", message.getContent());
        assertFalse(message.isDeleted());
    }

    @Test
    void initializeTextRejectsBlankContent() {
        ChatMessage message = new ChatMessage();
        message.setContent("   ");

        assertThrows(BadRequestException.class, () -> policy.initializeText(message));
    }

    @Test
    void initializeTextRejectsUnsupportedCharacters() {
        ChatMessage message = new ChatMessage();
        message.setContent("hello <script>");

        assertThrows(BadRequestException.class, () -> policy.initializeText(message));
    }

    @Test
    void assistantAndUserParagraphsPreserveLineBreaksAndNormalizeWindowsNewlines() {
        ChatMessage assistant = new ChatMessage();
        assistant.setContent("  1. Mở Phương tiện.\r\n2. Chọn Thêm xe.\r3. Nhập biển số.  ");
        policy.initializeAssistantText(assistant);
        assertEquals("1. Mở Phương tiện.\n2. Chọn Thêm xe.\n3. Nhập biển số.", assistant.getContent());
        ChatMessage user = new ChatMessage();
        user.setContent("Tôi đã thử.\nVẫn gặp lỗi.");
        policy.initializeText(user);
        assertEquals("Tôi đã thử.\nVẫn gặp lỗi.", user.getContent());
        assertThrows(BadRequestException.class,
                () -> TextValidationUtils.normalizeRequiredText("Tên\nkhông hợp lệ", "name", 100));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bước 1\n<script>alert(1)</script>", "Bước 1\u0000Bước 2", "Bước 1\tBước 2", "Bước 1\u007fBước 2"})
    void multilineAssistantStillRejectsMarkupAndOtherControlCharacters(String content) {
        ChatMessage message = new ChatMessage();
        message.setContent(content);
        assertThrows(BadRequestException.class, () -> policy.initializeAssistantText(message));
    }

    @Test
    void multilineAssistantStillEnforcesLengthAndNonblankContent() {
        ChatMessage message = new ChatMessage();
        message.setContent("x".repeat(ChatMessagePolicy.MAX_TEXT_CONTENT_LENGTH) + "\nx");
        assertThrows(BadRequestException.class, () -> policy.initializeAssistantText(message));
        message.setContent("\r\n  ");
        assertThrows(BadRequestException.class, () -> policy.initializeAssistantText(message));
    }

    @Test
    void initializeContextCardKeepsSafeSummaryAndMarksStructuredMessage() {
        ChatMessage message = new ChatMessage();
        message.setContent("  Đã chia sẻ đơn hàng Vé tháng  ");

        policy.initializeContextCard(message);

        assertEquals(ChatMessageType.CONTEXT_CARD, message.getMessageType());
        assertEquals("Đã chia sẻ đơn hàng Vé tháng", message.getContent());
        assertFalse(message.isDeleted());
    }
}
