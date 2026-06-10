package com.ban.vehicle_management.infrastructure.persistence.database.specification.iam;

import com.ban.vehicle_management.application.iam.account.model.command.ProvisionedAccountFilterCommand;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.RoleEntity;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class ProvisionedAccountSpecifications {

    private ProvisionedAccountSpecifications() {
    }

    public static Specification<AccountEntity> withFilters(ProvisionedAccountFilterCommand command) {
        return (root, query, criteriaBuilder) -> {
            Join<AccountEntity, RoleEntity> roleJoin = root.join("role", JoinType.INNER);

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(roleJoin.get("code").in(AdminProvisionableAccountRoleCode.codes()));

            if (command.roleCode() != null) {
                predicates.add(criteriaBuilder.equal(roleJoin.get("code"), command.roleCode().name()));
            }

            if (command.accountStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), command.accountStatus()));
            }

            if (command.keyword() != null && !command.keyword().isBlank()) {
                String keywordPattern = "%" + command.keyword().trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), keywordPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), keywordPattern)
                ));
            }

            query.orderBy(
                    criteriaBuilder.asc(root.get("username"))
            );

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
