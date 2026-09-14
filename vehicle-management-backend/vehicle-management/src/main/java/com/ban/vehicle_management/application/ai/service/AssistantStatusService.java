package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.port.in.AssistantStatusPortIn;
import com.ban.vehicle_management.application.ai.port.out.AssistantJobPortOut;
import com.ban.vehicle_management.application.ai.port.out.AiToolCallPortOut;
import com.ban.vehicle_management.domain.ai.model.AssistantJob;
import com.ban.vehicle_management.shared.enumeration.ai.AiToolCallStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AssistantJobStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AssistantStatusService implements AssistantStatusPortIn {

    private final AiAssistantProperties properties;
    private final AssistantJobPortOut assistantJobPortOut;
    private final AiToolCallPortOut aiToolCallPortOut;

    public AssistantStatusService(
            AiAssistantProperties properties,
            AssistantJobPortOut assistantJobPortOut,
            AiToolCallPortOut aiToolCallPortOut
    ) {
        this.properties = properties;
        this.assistantJobPortOut = assistantJobPortOut;
        this.aiToolCallPortOut = aiToolCallPortOut;
    }

    @Override
    public boolean isAssistantEnabled() {
        return properties.isProviderCallAllowed();
    }

    @Override
    public MessageStatus getMessageStatus(UUID inputMessageId) {
        if (!isAssistantEnabled()) {
            return new MessageStatus(inputMessageId, "DISABLED", null, true);
        }
        return assistantJobPortOut.findByInputMessageId(inputMessageId)
                .map(job -> new MessageStatus(
                        inputMessageId,
                        resolveStatus(inputMessageId, job),
                        job.getErrorCode(),
                        isTerminal(job)
                ))
                .orElseGet(() -> new MessageStatus(inputMessageId, "DISABLED", null, true));
    }

    private String resolveStatus(UUID inputMessageId, AssistantJob job) {
        boolean awaitingConfirmation = aiToolCallPortOut.findByInputMessageId(inputMessageId).stream()
                .anyMatch(toolCall -> toolCall.getStatus() == AiToolCallStatus.AWAITING_CONFIRMATION
                        && toolCall.getExpiresAt() != null
                        && Instant.now().isBefore(toolCall.getExpiresAt()));
        return awaitingConfirmation ? "WAITING_CONFIRMATION" : job.getStatus().name();
    }

    private boolean isTerminal(AssistantJob job) {
        return job.getStatus() == AssistantJobStatus.COMPLETED || job.getStatus() == AssistantJobStatus.FAILED;
    }
}
