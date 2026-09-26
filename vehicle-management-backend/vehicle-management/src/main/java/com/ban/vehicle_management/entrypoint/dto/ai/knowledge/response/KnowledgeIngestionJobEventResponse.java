package com.ban.vehicle_management.entrypoint.dto.ai.knowledge.response;

import java.time.Instant;

import com.ban.vehicle_management.shared.enumeration.ai.IngestionStage;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KnowledgeIngestionJobEventResponse {
    private Long eventId;
    private UUID ingestionJobId;
    private String eventType;
    private IngestionStage stage;
    private String detail;
    private Instant createdAt;
}
