package com.ban.vehicle_management.infrastructure.persistence.database.entity.ai;

import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIndexVersionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "knowledge_index_versions", schema = "ai")
@Getter
@Setter
public class KnowledgeIndexVersionEntity implements Persistable<UUID> {

    @Id
    @Column(name = "index_version_id", nullable = false)
    private UUID indexVersionId;

    @Column(name = "version_code", nullable = false, length = 120)
    private String versionCode;

    @Column(name = "model_configuration_id", nullable = false)
    private UUID modelConfigurationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 30)
    private AiProvider provider;

    @Column(name = "model_id", nullable = false, length = 120)
    private String modelId;

    @Column(name = "dimension", nullable = false)
    private Integer dimension;

    @Column(name = "chunker_version", nullable = false, length = 60)
    private String chunkerVersion;

    @Column(name = "embedding_prompt_version", nullable = false, length = 60)
    private String embeddingPromptVersion;

    @Column(name = "distance_metric", nullable = false, length = 20)
    private String distanceMetric;

    @Column(name = "normalization", nullable = false, length = 20)
    private String normalization;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private KnowledgeIndexVersionStatus status;

    @Column(name = "expected_chunk_count", nullable = false)
    private Integer expectedChunkCount;

    @Column(name = "embedded_chunk_count", nullable = false)
    private Integer embeddedChunkCount;

    @Column(name = "failed_chunk_count", nullable = false)
    private Integer failedChunkCount;

    @Column(name = "content_checksum", length = 64)
    private String contentChecksum;

    @Column(name = "failure_code", length = 80)
    private String failureCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "ready_at")
    private Instant readyAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "activated_by")
    private UUID activatedBy;

    @Column(name = "retired_at")
    private Instant retiredAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Version
    @Column(name = "version")
    private Integer version;

    @Override
    public UUID getId() {
        return indexVersionId;
    }

    @Override
    public boolean isNew() {
        return version == null;
    }
}
