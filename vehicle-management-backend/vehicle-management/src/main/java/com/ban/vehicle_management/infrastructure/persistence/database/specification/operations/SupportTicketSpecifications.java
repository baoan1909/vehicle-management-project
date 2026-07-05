package com.ban.vehicle_management.infrastructure.persistence.database.specification.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.SupportTicketEntity;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class SupportTicketSpecifications {

    private SupportTicketSpecifications() {
    }

    public static Specification<SupportTicketEntity> withFilters(
            UUID customerId,
            UUID categoryId,
            UUID assignedTo,
            SupportTicketStatus status,
            SupportTicketCategoryPriority priority,
            String keyword
    ) {
        return Specification
                .where(hasCustomerId(customerId))
                .and(hasCategoryId(categoryId))
                .and(hasAssignedTo(assignedTo))
                .and(hasStatus(status))
                .and(hasPriority(priority))
                .and(containsKeyword(keyword));
    }

    private static Specification<SupportTicketEntity> hasCustomerId(UUID customerId) {
        return (root, query, cb) -> customerId == null ? null : cb.equal(root.get("customerId"), customerId);
    }

    private static Specification<SupportTicketEntity> hasCategoryId(UUID categoryId) {
        return (root, query, cb) -> categoryId == null ? null : cb.equal(root.get("categoryId"), categoryId);
    }

    private static Specification<SupportTicketEntity> hasAssignedTo(UUID assignedTo) {
        return (root, query, cb) -> assignedTo == null ? null : cb.equal(root.get("assignedTo"), assignedTo);
    }

    private static Specification<SupportTicketEntity> hasStatus(SupportTicketStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    private static Specification<SupportTicketEntity> hasPriority(SupportTicketCategoryPriority priority) {
        return (root, query, cb) -> priority == null ? null : cb.equal(root.join("category").get("priority"), priority);
    }

    private static Specification<SupportTicketEntity> containsKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }

            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("content")), pattern),
                    cb.like(cb.lower(root.get("resolutionNote")), pattern)
            );
        };
    }
}