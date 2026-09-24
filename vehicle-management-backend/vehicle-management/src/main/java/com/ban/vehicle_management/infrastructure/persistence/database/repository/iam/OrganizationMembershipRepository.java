package com.ban.vehicle_management.infrastructure.persistence.database.repository.iam;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.OrganizationMembershipEntity;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationMembershipStatus;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembershipEntity, UUID> {
    Optional<OrganizationMembershipEntity> findByOrganizationIdAndAccountIdAndStatus(
            UUID organizationId,
            UUID accountId,
            OrganizationMembershipStatus status
    );

    @Query("""
        select membership.organizationId
        from OrganizationMembershipEntity membership
        where membership.accountId = :accountId
          and membership.status = :status
        """)
    Set<UUID> findActiveOrganizationIdsByAccountId(
            @Param("accountId") UUID accountId,
            @Param("status") OrganizationMembershipStatus status
    );
}
