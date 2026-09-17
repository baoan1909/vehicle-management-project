package com.ban.vehicle_management.domain.ai.knowledge.model;

import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeAccessScope;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeSourceStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Aggregates documents that belong together (for example a manual or a tag group).
 * TENANT_PRIVATE is not exposed in this phase: creation rejects that scope.
 */
@Getter
@Setter
public class KnowledgeSource {

    private UUID sourceId;
    private UUID tenantId;
    private String title;
    private String description;
    private KnowledgeAccessScope accessScope;
    private KnowledgeSourceStatus status;
    private UUID createdBy;
    private UUID updatedByAccountId;
    private Instant createdAt;
    private Instant updatedAt;

    public static KnowledgeSource create(
            UUID tenantId,
            String title,
            String description,
            KnowledgeAccessScope accessScope,
            UUID actor,
            Instant now) {
        KnowledgeSource source = new KnowledgeSource();
        source.sourceId = UUID.randomUUID();
        source.tenantId = tenantId;
        source.title = title;
        source.description = description;
        source.accessScope = accessScope;
        source.status = KnowledgeSourceStatus.ACTIVE;
        source.createdBy = actor;
        source.updatedByAccountId = actor;
        source.createdAt = now;
        source.updatedAt = now;
        return source;
    }

    public void rename(String title, String description, UUID actor, Instant now) {
        this.title = title;
        this.description = description;
        this.updatedByAccountId = actor;
        this.updatedAt = now;
    }

    public void changeScope(KnowledgeAccessScope accessScope, UUID actor, Instant now) {
        this.accessScope = accessScope;
        this.updatedByAccountId = actor;
        this.updatedAt = now;
    }

    public void deactivate(UUID actor, Instant now) {
        this.status = KnowledgeSourceStatus.INACTIVE;
        this.updatedByAccountId = actor;
        this.updatedAt = now;
    }

    public void reactivate(UUID actor, Instant now) {
        this.status = KnowledgeSourceStatus.ACTIVE;
        this.updatedByAccountId = actor;
        this.updatedAt = now;
    }
}