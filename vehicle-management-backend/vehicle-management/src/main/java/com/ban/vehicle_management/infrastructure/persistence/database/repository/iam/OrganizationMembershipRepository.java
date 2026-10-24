package com.ban.vehicle_management.infrastructure.persistence.database.repository.iam;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.OrganizationMembershipEntity;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationMembershipStatus;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembershipEntity, UUID> {
    Optional<OrganizationMembershipEntity> findByOrganizationIdAndAccountIdAndStatus(
            UUID organizationId,
            UUID accountId,
            OrganizationMembershipStatus status
    );

    @Query("""
        select membership.organizationId
        from OrganizationMembershipEntity membership, OrganizationEntity organization
        where membership.accountId = :accountId
          and membership.status = :status
          and organization.organizationId = membership.organizationId
          and organization.status = com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus.ACTIVE
        """)
    Set<UUID> findActiveOrganizationIdsByAccountId(
            @Param("accountId") UUID accountId,
            @Param("status") OrganizationMembershipStatus status
    );

    @Query("""
        select membership.organizationId
        from OrganizationMembershipEntity membership
        where membership.accountId = :accountId
        """)
    Set<UUID> findOrganizationIdsByAccountId(@Param("accountId") UUID accountId);

    @Query("""
        select count(membership)
        from OrganizationMembershipEntity membership
        join OrganizationEntity organization
            on organization.organizationId = membership.organizationId
        where membership.accountId = :accountId
          and membership.status = :membershipStatus
          and organization.status = :organizationStatus
        """)
    long countByAccountIdAndMembershipStatusAndOrganizationStatus(
            @Param("accountId") UUID accountId,
            @Param("membershipStatus") OrganizationMembershipStatus membershipStatus,
            @Param("organizationStatus") OrganizationStatus organizationStatus
    );

    default boolean hasActiveMembershipInActiveOrganization(UUID accountId) {
        return accountId != null
                && countByAccountIdAndMembershipStatusAndOrganizationStatus(
                        accountId,
                        OrganizationMembershipStatus.ACTIVE,
                        OrganizationStatus.ACTIVE
                ) > 0;
    }

    @Modifying
    @Query("""
        update OrganizationMembershipEntity membership
        set membership.status = :status
        where membership.accountId = :accountId
        """)
    int updateStatusByAccountId(
            @Param("accountId") UUID accountId,
            @Param("status") OrganizationMembershipStatus status
    );
}
