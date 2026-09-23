package com.ban.vehicle_management.application.ai.port.in;

import com.ban.vehicle_management.domain.ai.model.AiToolCall;
import java.util.UUID;

public interface AiToolCallPortIn {

    AiToolCall getToolCall(UUID toolCallId);

    AiToolCall confirm(UUID toolCallId);

    AiToolCall deny(UUID toolCallId);
}
