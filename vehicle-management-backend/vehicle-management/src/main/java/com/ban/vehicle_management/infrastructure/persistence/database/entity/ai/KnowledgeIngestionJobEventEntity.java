package com.ban.vehicle_management.infrastructure.persistence.database.entity.ai;

import com.ban.vehicle_management.shared.enumeration.ai.IngestionStage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "knowledge_ingestion_job_events", schema = "ai")
@Getter
@Setter
public class KnowledgeIngestionJobEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "ingestion_job_id", nullable = false)
    private UUID ingestionJobId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage")
    private IngestionStage stage;

    @Column(name = "detail")
    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}