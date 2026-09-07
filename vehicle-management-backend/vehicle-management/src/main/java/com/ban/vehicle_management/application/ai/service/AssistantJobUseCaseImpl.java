package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.port.in.AssistantJobPortIn;
import com.ban.vehicle_management.application.ai.port.out.AssistantJobPortOut;
import com.ban.vehicle_management.domain.ai.model.AssistantJob;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessage;
import com.ban.vehicle_management.shared.enumeration.ai.AssistantJobStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationType;
import com.ban.vehicle_management.shared.enumeration.operations.ChatMessageType;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssistantJobUseCaseImpl implements AssistantJobPortIn {

    private final AiAssistantProperties properties;
    private final AssistantJobPortOut assistantJobPortOut;

    public AssistantJobUseCaseImpl(
            AiAssistantProperties properties,
            AssistantJobPortOut assistantJobPortOut
    ) {
        this.properties = properties;
        this.assistantJobPortOut = assistantJobPortOut;
    }

    @Override
    @Transactional
    public void enqueueAssistantReply(ChatConversation conversation, ChatMessage inputMessage) {
        if (!properties.isProviderCallAllowed()
                || conversation == null
                || inputMessage == null
                || conversation.getConversationType() != ChatConversationType.ASSISTANT_SUPPORT
                || inputMessage.getMessageType() != ChatMessageType.TEXT
                || inputMessage.getSenderAccountId() == null) {
            return;
        }
        if (assistantJobPortOut.findByInputMessageId(inputMessage.getMessageId()).isPresent()) {
            return;
        }
        AssistantJob job = new AssistantJob();
        job.setJobId(UUID.randomUUID());
        job.setConversationId(conversation.getConversationId());
        job.setInputMessageId(inputMessage.getMessageId());
        job.setStatus(AssistantJobStatus.PENDING);
        job.setAttemptCount(0);
        job.setNextAttemptAt(Instant.now());
        job.setCreatedAt(Instant.now());
        assistantJobPortOut.save(job);
    }
}
