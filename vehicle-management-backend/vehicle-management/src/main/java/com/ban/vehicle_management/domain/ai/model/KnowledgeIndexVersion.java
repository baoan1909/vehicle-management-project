package com.ban.vehicle_management.domain.ai.model;

import com.ban.vehicle_management.domain.ai.policy.KnowledgeIndexVersionPolicy;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIndexVersionStatus;
import java.util.UUID;

/**
 * A knowledge index version snapshots exactly one embedding provider/model/dimension.
 * All state transitions are delegated to KnowledgeIndexVersionPolicy so invalid
 * transitions (e.g. DRAFT -> ACTIVE, FAILED -> ACTIVE) fail fast.
 */
public class KnowledgeIndexVersion {

    private UUID indexVersionId;
    private String versionCode;
    private UUID modelConfigurationId;
    private AiProvider provider;
    private String modelId;
    private int dimension;
    private String chunkerVersion;
    private String embeddingPromptVersion;
    private String distanceMetric;
    private String normalization;
    private KnowledgeIndexVersionStatus status;
    private int expectedChunkCount;
    private int embeddedChunkCount;
    private int failedChunkCount;
    private String contentChecksum;
    private String failureCode;
    private java.time.Instant createdAt;
    private UUID createdBy;
    private java.time.Instant readyAt;
    private java.time.Instant activatedAt;
    private UUID activatedBy;
    private java.time.Instant retiredAt;
    private java.time.Instant updatedAt;
    private UUID updatedBy;
    private Integer version;

    public static KnowledgeIndexVersion draft(
            String versionCode,
            UUID modelConfigurationId,
            AiProvider provider,
            String modelId,
            int dimension,
            String chunkerVersion,
            String embeddingPromptVersion,
            String distanceMetric,
            String normalization,
            String contentChecksum,
            UUID actor,
            java.time.Instant createdAt
    ) {
        if (modelConfigurationId == null) {
            throw new IllegalArgumentException("modelConfigurationId must not be null");
        }
        if (provider == null || modelId == null || modelId.isBlank()) {
            throw new IllegalArgumentException("provider and modelId are required");
        }
        if (dimension <= 0) {
            throw new IllegalArgumentException("dimension must be positive");
        }
        if (chunkerVersion == null || chunkerVersion.isBlank()) {
            throw new IllegalArgumentException("chunkerVersion must not be blank");
        }
        if (embeddingPromptVersion == null || embeddingPromptVersion.isBlank()) {
            throw new IllegalArgumentException("embeddingPromptVersion must not be blank");
        }
        if (distanceMetric == null || distanceMetric.isBlank()) {
            throw new IllegalArgumentException("distanceMetric must not be blank");
        }
        if (normalization == null || normalization.isBlank()) {
            throw new IllegalArgumentException("normalization must not be blank");
        }

        KnowledgeIndexVersion indexVersion = new KnowledgeIndexVersion();
        indexVersion.indexVersionId = java.util.UUID.randomUUID();
        indexVersion.versionCode = versionCode;
        indexVersion.modelConfigurationId = modelConfigurationId;
        indexVersion.provider = provider;
        indexVersion.modelId = modelId;
        indexVersion.dimension = dimension;
        indexVersion.chunkerVersion = chunkerVersion;
        indexVersion.embeddingPromptVersion = embeddingPromptVersion;
        indexVersion.distanceMetric = distanceMetric;
        indexVersion.normalization = normalization;
        indexVersion.status = KnowledgeIndexVersionStatus.DRAFT;
        indexVersion.expectedChunkCount = 0;
        indexVersion.embeddedChunkCount = 0;
        indexVersion.failedChunkCount = 0;
        indexVersion.contentChecksum = contentChecksum;
        indexVersion.createdAt = createdAt;
        indexVersion.createdBy = actor;
        indexVersion.version = 0;
        return indexVersion;
    }

    public void startBuild(UUID actor, java.time.Instant at) {
        KnowledgeIndexVersionPolicy.assertTransition(status, KnowledgeIndexVersionStatus.BUILDING);
        this.status = KnowledgeIndexVersionStatus.BUILDING;
        this.updatedAt = at;
        this.updatedBy = actor;
    }

    public void markReady(java.time.Instant at) {
        KnowledgeIndexVersionPolicy.assertTransition(status, KnowledgeIndexVersionStatus.READY);
        this.status = KnowledgeIndexVersionStatus.READY;
        this.readyAt = at;
        this.updatedAt = at;
    }

    public void fail(String failureCode, java.time.Instant at) {
        KnowledgeIndexVersionPolicy.assertTransition(status, KnowledgeIndexVersionStatus.FAILED);
        this.status = KnowledgeIndexVersionStatus.FAILED;
        this.failureCode = failureCode;
        this.updatedAt = at;
    }

    public void activate(UUID actor, java.time.Instant at) {
        KnowledgeIndexVersionPolicy.assertTransition(status, KnowledgeIndexVersionStatus.ACTIVE);
        this.status = KnowledgeIndexVersionStatus.ACTIVE;
        this.activatedAt = at;
        this.activatedBy = actor;
        this.updatedAt = at;
        this.updatedBy = actor;
    }

    public void retire(UUID actor, java.time.Instant at) {
        KnowledgeIndexVersionPolicy.assertTransition(status, KnowledgeIndexVersionStatus.RETIRED);
        this.status = KnowledgeIndexVersionStatus.RETIRED;
        this.retiredAt = at;
        this.updatedAt = at;
        this.updatedBy = actor;
    }

    public boolean isBuildComplete() {
        return failedChunkCount == 0 && embeddedChunkCount >= expectedChunkCount;
    }

    public UUID getIndexVersionId() {
        return indexVersionId;
    }

    public void setIndexVersionId(UUID indexVersionId) {
        this.indexVersionId = indexVersionId;
    }

    public String getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(String versionCode) {
        this.versionCode = versionCode;
    }

    public UUID getModelConfigurationId() {
        return modelConfigurationId;
    }

    public void setModelConfigurationId(UUID modelConfigurationId) {
        this.modelConfigurationId = modelConfigurationId;
    }

    public AiProvider getProvider() {
        return provider;
    }

    public void setProvider(AiProvider provider) {
        this.provider = provider;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    public String getChunkerVersion() {
        return chunkerVersion;
    }

    public void setChunkerVersion(String chunkerVersion) {
        this.chunkerVersion = chunkerVersion;
    }

    public String getEmbeddingPromptVersion() {
        return embeddingPromptVersion;
    }

    public void setEmbeddingPromptVersion(String embeddingPromptVersion) {
        this.embeddingPromptVersion = embeddingPromptVersion;
    }

    public String getDistanceMetric() {
        return distanceMetric;
    }

    public void setDistanceMetric(String distanceMetric) {
        this.distanceMetric = distanceMetric;
    }

    public String getNormalization() {
        return normalization;
    }

    public void setNormalization(String normalization) {
        this.normalization = normalization;
    }

    public KnowledgeIndexVersionStatus getStatus() {
        return status;
    }

    public void setStatus(KnowledgeIndexVersionStatus status) {
        this.status = status;
    }

    public int getExpectedChunkCount() {
        return expectedChunkCount;
    }

    public void setExpectedChunkCount(int expectedChunkCount) {
        this.expectedChunkCount = expectedChunkCount;
    }

    public int getEmbeddedChunkCount() {
        return embeddedChunkCount;
    }

    public void setEmbeddedChunkCount(int embeddedChunkCount) {
        this.embeddedChunkCount = embeddedChunkCount;
    }

    public int getFailedChunkCount() {
        return failedChunkCount;
    }

    public void setFailedChunkCount(int failedChunkCount) {
        this.failedChunkCount = failedChunkCount;
    }

    public String getContentChecksum() {
        return contentChecksum;
    }

    public void setContentChecksum(String contentChecksum) {
        this.contentChecksum = contentChecksum;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public void setFailureCode(String failureCode) {
        this.failureCode = failureCode;
    }

    public java.time.Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public java.time.Instant getReadyAt() {
        return readyAt;
    }

    public void setReadyAt(java.time.Instant readyAt) {
        this.readyAt = readyAt;
    }

    public java.time.Instant getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(java.time.Instant activatedAt) {
        this.activatedAt = activatedAt;
    }

    public UUID getActivatedBy() {
        return activatedBy;
    }

    public void setActivatedBy(UUID activatedBy) {
        this.activatedBy = activatedBy;
    }

    public java.time.Instant getRetiredAt() {
        return retiredAt;
    }

    public void setRetiredAt(java.time.Instant retiredAt) {
        this.retiredAt = retiredAt;
    }

    public java.time.Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(java.time.Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
