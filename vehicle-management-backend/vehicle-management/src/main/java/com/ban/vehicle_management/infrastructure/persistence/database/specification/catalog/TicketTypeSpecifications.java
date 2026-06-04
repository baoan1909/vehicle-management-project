package com.ban.vehicle_management.infrastructure.persistence.database.specification.catalog;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.TicketTypeEntity;
import com.ban.vehicle_management.shared.enumeration.catalog.TicketTypeStatus;
import org.springframework.data.jpa.domain.Specification;

public class TicketTypeSpecifications {
    private  TicketTypeSpecifications(){
    }

    public static Specification<TicketTypeEntity> withFilters(TicketTypeStatus status, String keyword){
        return  Specification
                .where(hasStatus(status))
                .and(containsKeyword(keyword));
    }

    private static  Specification<TicketTypeEntity> hasStatus(TicketTypeStatus status){
        return ((root, query, criteriaBuilder) -> status == null ? null : criteriaBuilder.equal(root.get("status"), status));
    }

    private static Specification<TicketTypeEntity> containsKeyword(String keyword){
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
