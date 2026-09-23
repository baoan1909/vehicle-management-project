package com.ban.vehicle_management.entrypoint.dto.ai.toolcall.response;

import com.ban.vehicle_management.shared.enumeration.ai.AiToolCallStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiToolType;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiToolCallResponse {
    private UUID toolCallId;
    private UUID conversationId;
    private UUID inputMessageId;
    private String toolName;
    private AiToolType toolType;
    private AiToolCallStatus status;
    private String requestPayloadRedacted;
    private String responsePayloadRedacted;
    private String expiresAt;
    private String failureCode;
}
