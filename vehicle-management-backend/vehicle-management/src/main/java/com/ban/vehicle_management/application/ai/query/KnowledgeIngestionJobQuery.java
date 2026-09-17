package com.ban.vehicle_management.application.ai.query;

import com.ban.vehicle_management.shared.enumeration.ai.IngestionStage;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIngestionJobStatus;
import java.time.Instant;
import java.util.UUID;

public record KnowledgeIngestionJobQuery(
        UUID documentId,
        KnowledgeIngestionJobStatus status,
        IngestionStage currentStage,
        String errorCode,
        Instant createdFrom,
        Instant createdTo,
        String keyword) {
}
