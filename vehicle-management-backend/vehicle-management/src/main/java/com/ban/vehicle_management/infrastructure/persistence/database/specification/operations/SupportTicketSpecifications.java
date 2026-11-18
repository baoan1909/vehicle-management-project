package com.ban.vehicle_management.infrastructure.persistence.database.specification.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.SupportTicketEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Join;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class SupportTicketSpecifications {

    private SupportTicketSpecifications() {
    }

    public static Specification<SupportTicketEntity> withFilters(
            UUID requesterAccountId,
            String requesterRoleCode,
            UUID categoryId,
            UUID assignedTo,
            UUID organizationId,
            UUID parkingLotId,
            String featureCode,
            SupportTicketStatus status,
            SupportTicketCategoryPriority priority,
            String keyword
    ) {
        return Specification
                .where(businessVisible())
                .and(hasRequesterAccountId(requesterAccountId))
                .and(hasRequesterRoleCode(requesterRoleCode))
                .and(hasCategoryId(categoryId))
                .and(hasAssignedTo(assignedTo))
                .and(hasOrganizationId(organizationId))
                .and(hasParkingLotId(parkingLotId))
                .and(hasFeatureCode(featureCode))
                .and(hasStatus(status))
                .and(hasPriority(priority))
                .and(containsKeyword(keyword));
    }

    public static Specification<SupportTicketEntity> businessVisible() {
        return (root, query, cb) -> {
            Join<SupportTicketEntity, AccountEntity> account = root.join("requesterAccount", JoinType.INNER);
            return cb.not(account.get("status").in(
                    AccountStatus.PENDING_DELETION, AccountStatus.DELETED));
        };
    }

    private static Specification<SupportTicketEntity> hasRequesterAccountId(UUID requesterAccountId) {
        return (root, query, cb) -> requesterAccountId == null
                ? null : cb.equal(root.get("requesterAccountId"), requesterAccountId);
    }

    private static Specification<SupportTicketEntity> hasRequesterRoleCode(String requesterRoleCode) {
        return (root, query, cb) -> requesterRoleCode == null || requesterRoleCode.isBlank()
                ? null : cb.equal(root.get("requesterRoleCode"), requesterRoleCode.trim());
    }

    private static Specification<SupportTicketEntity> hasCategoryId(UUID categoryId) {
        return (root, query, cb) -> categoryId == null ? null : cb.equal(root.get("categoryId"), categoryId);
    }

    private static Specification<SupportTicketEntity> hasAssignedTo(UUID assignedTo) {
        return (root, query, cb) -> assignedTo == null ? null : cb.equal(root.get("assignedTo"), assignedTo);
    }

    private static Specification<SupportTicketEntity> hasOrganizationId(UUID organizationId) {
        return (root, query, cb) -> organizationId == null ? null : cb.equal(root.get("organizationId"), organizationId);
    }

    private static Specification<SupportTicketEntity> hasParkingLotId(UUID parkingLotId) {
        return (root, query, cb) -> parkingLotId == null ? null : cb.equal(root.get("parkingLotId"), parkingLotId);
    }

    private static Specification<SupportTicketEntity> hasFeatureCode(String featureCode) {
        return (root, query, cb) -> featureCode == null || featureCode.isBlank()
                ? null : cb.equal(root.get("featureCode"), featureCode.trim());
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
