package com.ban.vehicle_management.application.ai.port.in;

import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessage;

public interface AssistantJobPortIn {

    void enqueueAssistantReply(ChatConversation conversation, ChatMessage inputMessage);
}
