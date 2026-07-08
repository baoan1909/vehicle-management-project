package com.ban.vehicle_management.application.operations.chatconversation.model;

import java.util.UUID;

public record ChatAttachmentReadUrl(
        UUID attachmentId,
        String readUrl,
        int expireSeconds
) {
}
