package com.ban.vehicle_management.infrastructure.persistence.database.repository.iam;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.MemberParkingLotScopeEntity;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationMembershipStatus;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberParkingLotScopeRepository extends JpaRepository<MemberParkingLotScopeEntity, UUID> {

    @Modifying
    void deleteByOrganizationMembershipId(UUID organizationMembershipId);

    Set<MemberParkingLotScopeEntity> findByOrganizationMembershipId(UUID organizationMembershipId);

    @Modifying
    long deleteByOrganizationMembershipIdAndParkingLotId(
            UUID organizationMembershipId,
            UUID parkingLotId
    );

    @Modifying
    long deleteByOrganizationMembershipIdAndParkingLotIdIn(
            UUID organizationMembershipId,
            Collection<UUID> parkingLotIds
    );

    @Modifying
    @Query(value = """
        insert into iam.member_parking_lot_scopes (
            member_parking_lot_scope_id,
            organization_membership_id,
            parking_lot_id,
            created_at
        ) values (
            :scopeId,
            :organizationMembershipId,
            :parkingLotId,
            now()
        )
        on conflict (organization_membership_id, parking_lot_id) do nothing
        """, nativeQuery = true)
    int insertIfAbsent(
            @Param("scopeId") UUID scopeId,
            @Param("organizationMembershipId") UUID organizationMembershipId,
            @Param("parkingLotId") UUID parkingLotId
    );

    @Query("""
        select scope.parkingLotId
        from MemberParkingLotScopeEntity scope, OrganizationMembershipEntity membership, OrganizationEntity organization
        where scope.organizationMembershipId = membership.organizationMembershipId
          and membership.accountId = :accountId
          and membership.status = :status
          and organization.organizationId = membership.organizationId
          and organization.status = com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus.ACTIVE
        """)
    Set<UUID> findActiveParkingLotIdsByAccountId(
            @Param("accountId") UUID accountId,
            @Param("status") OrganizationMembershipStatus status
    );

    @Query("""
        select scope.parkingLotId
        from MemberParkingLotScopeEntity scope, OrganizationMembershipEntity membership
        where scope.organizationMembershipId = membership.organizationMembershipId
          and membership.organizationId = :organizationId
          and membership.accountId = :accountId
          and membership.status = :status
        """)
    Set<UUID> findActiveParkingLotIdsByOrganizationIdAndAccountId(
            @Param("organizationId") UUID organizationId,
            @Param("accountId") UUID accountId,
            @Param("status") OrganizationMembershipStatus status
    );

    @Query("""
        select membership.accountId
        from MemberParkingLotScopeEntity scope, OrganizationMembershipEntity membership, AccountEntity account, RoleEntity role
        where scope.organizationMembershipId = membership.organizationMembershipId
          and membership.accountId = account.accountId
          and account.roleId = role.roleId
          and scope.parkingLotId = :parkingLotId
          and membership.status = :membershipStatus
          and account.status = :accountStatus
          and role.code = :roleCode
          and role.isActive = true
        """)
    Set<UUID> findActiveAccountIdsByParkingLotId(
            @Param("parkingLotId") UUID parkingLotId,
            @Param("membershipStatus") OrganizationMembershipStatus membershipStatus,
            @Param("accountStatus") com.ban.vehicle_management.shared.enumeration.iam.AccountStatus accountStatus,
            @Param("roleCode") String roleCode
    );

    @Query("""
        select distinct membership.accountId
        from MemberParkingLotScopeEntity scope, OrganizationMembershipEntity membership,
             OrganizationEntity organization, AccountEntity account, RoleEntity role
        where scope.organizationMembershipId = membership.organizationMembershipId
          and membership.organizationId = organization.organizationId
          and membership.accountId = account.accountId
          and account.roleId = role.roleId
          and scope.parkingLotId = :parkingLotId
          and membership.status = :membershipStatus
          and organization.status = com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus.ACTIVE
          and account.status = :accountStatus
          and role.code in :roleCodes
          and role.isActive = true
        """)
    Set<UUID> findActiveAccountIdsByParkingLotIdAndRoleCodeIn(
            @Param("parkingLotId") UUID parkingLotId,
            @Param("membershipStatus") OrganizationMembershipStatus membershipStatus,
            @Param("accountStatus") com.ban.vehicle_management.shared.enumeration.iam.AccountStatus accountStatus,
            @Param("roleCodes") Collection<String> roleCodes
    );

    @Modifying
    @Query(value = """
        delete from iam.member_parking_lot_scopes scope
        using iam.organization_memberships membership
        where scope.organization_membership_id = membership.organization_membership_id
          and membership.account_id = :accountId
        """, nativeQuery = true)
    int deleteAllByAccountId(@Param("accountId") UUID accountId);

}
