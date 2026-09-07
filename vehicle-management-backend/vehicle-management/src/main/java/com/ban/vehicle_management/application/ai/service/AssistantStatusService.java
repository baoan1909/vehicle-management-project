package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.port.in.AssistantStatusPortIn;
import com.ban.vehicle_management.application.ai.port.out.AssistantJobPortOut;
import com.ban.vehicle_management.domain.ai.model.AssistantJob;
import com.ban.vehicle_management.shared.enumeration.ai.AssistantJobStatus;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AssistantStatusService implements AssistantStatusPortIn {

    private final AiAssistantProperties properties;
    private final AssistantJobPortOut assistantJobPortOut;

    public AssistantStatusService(AiAssistantProperties properties, AssistantJobPortOut assistantJobPortOut) {
        this.properties = properties;
        this.assistantJobPortOut = assistantJobPortOut;
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
                        job.getStatus().name(),
                        job.getErrorCode(),
                        isTerminal(job)
                ))
                .orElseGet(() -> new MessageStatus(inputMessageId, "DISABLED", null, true));
    }

    private boolean isTerminal(AssistantJob job) {
        return job.getStatus() == AssistantJobStatus.COMPLETED || job.getStatus() == AssistantJobStatus.FAILED;
    }
}
