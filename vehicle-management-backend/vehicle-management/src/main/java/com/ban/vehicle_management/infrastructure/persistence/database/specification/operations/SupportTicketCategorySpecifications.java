package com.ban.vehicle_management.infrastructure.persistence.database.specification.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.SupportTicketCategoryEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.SupportTicketEntity;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryStatus;
import org.springframework.data.jpa.domain.Specification;

public final class SupportTicketCategorySpecifications {

    private SupportTicketCategorySpecifications(){}

    public static Specification<SupportTicketCategoryEntity> withFilters(
            SupportTicketCategoryStatus status,
            SupportTicketCategoryPriority priority,
            String keyword
    ) {
        return Specification
                .where(hasStatus(status))
                .and(hasPriority(priority))
                .and(containsKeyword(keyword));
    }

    private static Specification<SupportTicketCategoryEntity> hasStatus(SupportTicketCategoryStatus status){
        return ((root, query, criteriaBuilder) -> status == null ? null : criteriaBuilder.equal(root.get("status"), status));
    }

    private static Specification<SupportTicketCategoryEntity> hasPriority(SupportTicketCategoryPriority priority){
        return ((root, query, criteriaBuilder) -> priority == null ? null : criteriaBuilder.equal(root.get("priority"), priority));
    }

    private static  Specification<SupportTicketCategoryEntity> containsKeyword(String keyword){
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.isBlank()){
                return null;
            }

            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return  criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern)
            );
        };
    }
}
