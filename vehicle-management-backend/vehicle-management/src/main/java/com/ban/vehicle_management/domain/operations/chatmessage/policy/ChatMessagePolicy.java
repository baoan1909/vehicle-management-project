package com.ban.vehicle_management.domain.operations.chatmessage.policy;

import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessage;
import com.ban.vehicle_management.shared.enumeration.operations.ChatMessageType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;

public class ChatMessagePolicy {

    public static final int MAX_TEXT_CONTENT_LENGTH = 4_000;

    public void initializeText(ChatMessage message) {
        if (message == null) {
            throw new BadRequestException("message must not be null");
        }
        message.setMessageType(ChatMessageType.TEXT);
        message.setContent(TextValidationUtils.normalizeRequiredText(
                message.getContent(),
                "content",
                MAX_TEXT_CONTENT_LENGTH
        ));
        message.setDeleted(false);
    }

    public void initializeImage(ChatMessage message) {
        if (message == null) {
            throw new BadRequestException("message must not be null");
        }
        message.setMessageType(ChatMessageType.IMAGE);
        message.setContent(TextValidationUtils.normalizeNullableText(
                message.getContent(),
                "content",
                MAX_TEXT_CONTENT_LENGTH
        ));
        message.setDeleted(false);
    }
}
