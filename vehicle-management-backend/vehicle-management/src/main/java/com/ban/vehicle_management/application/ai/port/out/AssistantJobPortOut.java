package com.ban.vehicle_management.application.ai.port.out;

import com.ban.vehicle_management.domain.ai.model.AssistantJob;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssistantJobPortOut {

    Optional<AssistantJob> findByInputMessageId(UUID inputMessageId);

    AssistantJob save(AssistantJob job);

    List<AssistantJob> claimDueJobs(Instant now, Instant lockExpiresAt, String lockedBy, int limit);
}
