package com.ban.vehicle_management.infrastructure.persistence.specification.ai;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.KnowledgeSourceEntity;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeAccessScope;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeSourceStatus;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Specifications for filtering knowledge sources.
 */
public final class KnowledgeSourceSpecifications {

    private KnowledgeSourceSpecifications() {
    }

    public static Specification<KnowledgeSourceEntity> hasStatus(KnowledgeSourceStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<KnowledgeSourceEntity> hasAccessScope(KnowledgeAccessScope scope) {
        return scope == null ? null : (root, query, cb) -> cb.equal(root.get("accessScope"), scope);
    }

    public static Specification<KnowledgeSourceEntity> hasTenantId(UUID tenantId) {
        return tenantId == null ? null : (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }

    public static Specification<KnowledgeSourceEntity> keyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.like(cb.lower(root.get("title")), pattern));
            predicates.add(cb.like(cb.lower(root.get("description")), pattern));
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<KnowledgeSourceEntity> combine(
            Specification<KnowledgeSourceEntity>... specs) {
        Specification<KnowledgeSourceEntity> result = null;
        for (Specification<KnowledgeSourceEntity> spec : specs) {
            if (spec != null) {
                result = (result == null) ? spec : result.and(spec);
            }
        }
        return result;
    }

    public record Filter(
            KnowledgeSourceStatus status,
            KnowledgeAccessScope accessScope,
            UUID tenantId,
            String keyword) {

        public Specification<KnowledgeSourceEntity> toSpecification() {
            return combine(
                    hasStatus(status),
                    hasAccessScope(accessScope),
                    hasTenantId(tenantId),
                    KnowledgeSourceSpecifications.keyword(keyword));
        }
    }
}
