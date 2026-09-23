package com.ban.vehicle_management.application.ai.port.in;

import java.util.UUID;

public interface AssistantStatusPortIn {

    boolean isAssistantEnabled();

    MessageStatus getMessageStatus(UUID inputMessageId);

    record MessageStatus(UUID inputMessageId, String status, String errorCode, boolean terminal) {
    }
}
