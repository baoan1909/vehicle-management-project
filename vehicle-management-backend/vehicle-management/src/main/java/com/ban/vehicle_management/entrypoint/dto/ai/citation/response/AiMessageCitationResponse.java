package com.ban.vehicle_management.entrypoint.dto.ai.citation.response;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiMessageCitationResponse {
    private UUID citationId;
    private UUID messageId;
    private UUID documentId;
    private UUID chunkId;
    private String label;
    private String title;
    private Integer sourcePage;
    private String sourceSection;
    private BigDecimal retrievalScore;
    private UUID retrievalAuditId;
    private UUID indexVersionId;
    private int citationOrder;
}
