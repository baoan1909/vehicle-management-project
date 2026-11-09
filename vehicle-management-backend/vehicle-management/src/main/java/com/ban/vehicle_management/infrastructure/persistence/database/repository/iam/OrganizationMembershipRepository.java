package com.ban.vehicle_management.infrastructure.persistence.database.repository.iam;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.OrganizationMembershipEntity;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationMembershipStatus;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus;
import com.ban.vehicle_management.infrastructure.persistence.database.projection.iam.AccountDeletionOrganizationGovernanceProjection;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembershipEntity, UUID> {

    @Query(value = """
        select membership.organization_membership_id as "membershipId",
               membership.organization_id as "organizationId"
          from iam.organization_memberships membership
          join iam.organizations organization on organization.organization_id=membership.organization_id
         where membership.account_id=:accountId and membership.status='ACTIVE' and organization.status='ACTIVE'
           and not exists (
             select 1 from iam.organization_memberships successor_membership
               join iam.accounts successor on successor.account_id=successor_membership.account_id
               join iam.roles successor_role on successor_role.role_id=successor.role_id and successor_role.is_active=true
              where successor_membership.organization_id=membership.organization_id
                and successor_membership.status='ACTIVE' and successor.account_id<>:accountId
                and successor.status='ACTIVE'
                and (select count(distinct permission.permission_code)
                       from iam.role_permissions grant_row
                       join iam.permissions permission on permission.permission_id=grant_row.permission_id
                      where grant_row.role_id=successor.role_id and grant_row.is_active=true
                        and permission.permission_code in ('ORGANIZATION_MEMBERSHIP_MANAGE_ALL','EMPLOYEE_UPDATE_ALL'))=2)
        """, nativeQuery = true)
    List<AccountDeletionOrganizationGovernanceProjection> findDeletionGovernanceBlockers(
            @Param("accountId") UUID accountId);
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

    @Modifying
    @Query(value = """
        update iam.organization_memberships set status='INACTIVE', updated_at=:now
        where account_id=:accountId and status='ACTIVE'
        """, nativeQuery = true)
    int deactivateForDeletedAccount(@Param("accountId") UUID accountId, @Param("now") Instant now);
}
