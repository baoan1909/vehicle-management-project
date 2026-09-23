package com.ban.vehicle_management.entrypoint.dto.ai.citation.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageCitationsResponse {
    private UUID messageId;
    private List<AiMessageCitationResponse> citations;
    private BigDecimal groundedConfidence;
    private boolean handoffRecommended;
    private String diagnosticCode;
}
