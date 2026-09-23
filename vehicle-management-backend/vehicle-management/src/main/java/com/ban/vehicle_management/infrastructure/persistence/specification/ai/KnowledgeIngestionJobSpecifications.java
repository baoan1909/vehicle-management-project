package com.ban.vehicle_management.infrastructure.persistence.specification.ai;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.KnowledgeIngestionJobEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.KnowledgeDocumentEntity;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIngestionJobStatus;
import com.ban.vehicle_management.shared.enumeration.ai.IngestionStage;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Specifications for filtering knowledge ingestion jobs.
 */
public final class KnowledgeIngestionJobSpecifications {

    private KnowledgeIngestionJobSpecifications() {
    }

    public static Specification<KnowledgeIngestionJobEntity> hasDocumentId(UUID documentId) {
        return documentId == null ? null : (root, query, cb) -> cb.equal(root.get("documentId"), documentId);
    }

    public static Specification<KnowledgeIngestionJobEntity> hasStatus(KnowledgeIngestionJobStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<KnowledgeIngestionJobEntity> hasCurrentStage(IngestionStage stage) {
        return stage == null ? null : (root, query, cb) -> cb.equal(root.get("currentStage"), stage);
    }

    public static Specification<KnowledgeIngestionJobEntity> hasErrorCode(String errorCode) {
        if (!StringUtils.hasText(errorCode)) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("errorCode"), errorCode);
    }

    public static Specification<KnowledgeIngestionJobEntity> createdFrom(Instant from) {
        return from == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<KnowledgeIngestionJobEntity> createdTo(Instant to) {
        return to == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    public static Specification<KnowledgeIngestionJobEntity> keyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> {
            Subquery<UUID> documentIds = query.subquery(UUID.class);
            Root<KnowledgeDocumentEntity> document = documentIds.from(KnowledgeDocumentEntity.class);
            documentIds.select(document.get("documentId"));
            documentIds.where(
                    cb.equal(document.get("documentId"), root.get("documentId")),
                    cb.or(
                            cb.like(cb.lower(document.get("title")), pattern),
                            cb.like(cb.lower(document.get("originalFilename")), pattern)));
            return cb.exists(documentIds);
        };
    }

    public static Specification<KnowledgeIngestionJobEntity> combine(
            Specification<KnowledgeIngestionJobEntity>... specs) {
        Specification<KnowledgeIngestionJobEntity> result = null;
        for (Specification<KnowledgeIngestionJobEntity> spec : specs) {
            if (spec != null) {
                result = (result == null) ? spec : result.and(spec);
            }
        }
        return result;
    }

    public record Filter(
            UUID documentId,
            KnowledgeIngestionJobStatus status,
            IngestionStage currentStage,
            String errorCode,
            Instant createdFrom,
            Instant createdTo,
            String keyword) {

        public Specification<KnowledgeIngestionJobEntity> toSpecification() {
            return combine(
                    hasDocumentId(documentId),
                    hasStatus(status),
                    hasCurrentStage(currentStage),
                    hasErrorCode(errorCode),
                    KnowledgeIngestionJobSpecifications.createdFrom(createdFrom),
                    KnowledgeIngestionJobSpecifications.createdTo(createdTo),
                    KnowledgeIngestionJobSpecifications.keyword(keyword));
        }
    }
}
