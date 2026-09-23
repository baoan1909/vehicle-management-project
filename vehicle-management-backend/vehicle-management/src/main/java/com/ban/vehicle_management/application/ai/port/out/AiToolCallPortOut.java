package com.ban.vehicle_management.application.ai.port.out;

import com.ban.vehicle_management.domain.ai.model.AiToolCall;
import com.ban.vehicle_management.shared.enumeration.ai.AiToolCallStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiToolCallPortOut {

    AiToolCall save(AiToolCall toolCall);

    Optional<AiToolCall> findById(UUID toolCallId);

    Optional<AiToolCall> findByIdForUpdate(UUID toolCallId);

    List<AiToolCall> findByInputMessageId(UUID inputMessageId);

    List<AiToolCall> findExpired(AiToolCallStatus status, Instant now);
}
