package com.ban.vehicle_management.domain.ai.knowledge.model;

import com.ban.vehicle_management.shared.enumeration.ai.IngestionStage;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Append-only trail of a job for the admin UI and diagnostics; never mutated.
 */
@Getter
@Setter
public class KnowledgeIngestionJobEvent {

    private Long eventId;
    private UUID ingestionJobId;
    private String eventType;
    private IngestionStage stage;
    private String detail;
    private Instant createdAt;

    public static KnowledgeIngestionJobEvent of(
            UUID ingestionJobId, String eventType, IngestionStage stage, String detail, Instant now) {
        KnowledgeIngestionJobEvent event = new KnowledgeIngestionJobEvent();
        event.ingestionJobId = ingestionJobId;
        event.eventType = eventType;
        event.stage = stage;
        event.detail = detail;
        event.createdAt = now;
        return event;
    }
}