package com.ban.vehicle_management.infrastructure.persistence.specification.ai;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.KnowledgeDocumentEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.KnowledgeSourceEntity;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeAccessScope;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeDocumentStatus;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Specifications for filtering knowledge documents.
 * All dynamic filters must use Specification to push filtering to the database.
 */
public final class KnowledgeDocumentSpecifications {

    private KnowledgeDocumentSpecifications() {
    }

    public static Specification<KnowledgeDocumentEntity> hasSourceId(UUID sourceId) {
        return sourceId == null ? null : (root, query, cb) -> cb.equal(root.get("sourceId"), sourceId);
    }

    public static Specification<KnowledgeDocumentEntity> hasDocumentKey(UUID documentKey) {
        return documentKey == null ? null : (root, query, cb) -> cb.equal(root.get("documentKey"), documentKey);
    }

    public static Specification<KnowledgeDocumentEntity> hasStatus(KnowledgeDocumentStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<KnowledgeDocumentEntity> hasAccessScope(KnowledgeAccessScope scope) {
        return scope == null ? null : (root, query, cb) -> cb.equal(root.get("accessScope"), scope);
    }

    public static Specification<KnowledgeDocumentEntity> hasTenantId(UUID tenantId) {
        return tenantId == null ? null : (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }

    public static Specification<KnowledgeDocumentEntity> hasFileExtension(String extension) {
        if (!StringUtils.hasText(extension)) {
            return null;
        }
        String normalized = extension.trim().toLowerCase(Locale.ROOT);
        return (root, query, cb) -> cb.equal(cb.lower(root.get("fileExtension")), normalized);
    }

    public static Specification<KnowledgeDocumentEntity> createdFrom(Instant from) {
        return from == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<KnowledgeDocumentEntity> createdTo(Instant to) {
        return to == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    public static Specification<KnowledgeDocumentEntity> keyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> {
            Subquery<UUID> matchingSources = query.subquery(UUID.class);
            Root<KnowledgeSourceEntity> source = matchingSources.from(KnowledgeSourceEntity.class);
            matchingSources.select(source.get("sourceId"));
            matchingSources.where(
                    cb.equal(source.get("sourceId"), root.get("sourceId")),
                    cb.like(cb.lower(source.get("title")), pattern));
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.like(cb.lower(root.get("title")), pattern));
            predicates.add(cb.like(cb.lower(root.get("originalFilename")), pattern));
            predicates.add(cb.exists(matchingSources));
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<KnowledgeDocumentEntity> combine(
            Specification<KnowledgeDocumentEntity>... specs) {
        Specification<KnowledgeDocumentEntity> result = null;
        for (Specification<KnowledgeDocumentEntity> spec : specs) {
            if (spec != null) {
                result = (result == null) ? spec : result.and(spec);
            }
        }
        return result;
    }

    public static class Filter {
        private UUID sourceId;
        private UUID documentKey;
        private KnowledgeDocumentStatus status;
        private KnowledgeAccessScope accessScope;
        private UUID tenantId;
        private String fileExtension;
        private Instant createdFrom;
        private Instant createdTo;
        private String keyword;

        public Filter() {}

        public Filter(UUID sourceId, UUID documentKey, KnowledgeDocumentStatus status,
                      KnowledgeAccessScope accessScope, UUID tenantId, String fileExtension,
                      Instant createdFrom, Instant createdTo, String keyword) {
            this.sourceId = sourceId;
            this.documentKey = documentKey;
            this.status = status;
            this.accessScope = accessScope;
            this.tenantId = tenantId;
            this.fileExtension = fileExtension;
            this.createdFrom = createdFrom;
            this.createdTo = createdTo;
            this.keyword = keyword;
        }

        // Getters and setters
        public UUID getSourceId() { return sourceId; }
        public void setSourceId(UUID sourceId) { this.sourceId = sourceId; }
        public UUID getDocumentKey() { return documentKey; }
        public void setDocumentKey(UUID documentKey) { this.documentKey = documentKey; }
        public KnowledgeDocumentStatus getStatus() { return status; }
        public void setStatus(KnowledgeDocumentStatus status) { this.status = status; }
        public KnowledgeAccessScope getAccessScope() { return accessScope; }
        public void setAccessScope(KnowledgeAccessScope accessScope) { this.accessScope = accessScope; }
        public UUID getTenantId() { return tenantId; }
        public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
        public String getFileExtension() { return fileExtension; }
        public void setFileExtension(String fileExtension) { this.fileExtension = fileExtension; }
        public Instant getCreatedFrom() { return createdFrom; }
        public void setCreatedFrom(Instant createdFrom) { this.createdFrom = createdFrom; }
        public Instant getCreatedTo() { return createdTo; }
        public void setCreatedTo(Instant createdTo) { this.createdTo = createdTo; }
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }

        public Specification<KnowledgeDocumentEntity> toSpecification() {
            return combine(
                    hasSourceId(sourceId),
                    hasDocumentKey(documentKey),
                    hasStatus(status),
                    hasAccessScope(accessScope),
                    hasTenantId(tenantId),
                    hasFileExtension(fileExtension),
                    createdFrom(createdFrom),
                    createdTo(createdTo),
                    keyword(keyword)
            );
        }
    }
}
