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

    @Query(value = """
        select assignment.parking_lot_id
        from iam.parking_lot_management_assignments assignment
        join iam.organization_memberships membership
          on membership.organization_id = assignment.organization_id
         and membership.account_id = assignment.manager_account_id
        join iam.organizations organization on organization.organization_id = assignment.organization_id
        where assignment.manager_account_id = :accountId
          and assignment.status = 'ACTIVE'
          and assignment.effective_from <= now()
          and (assignment.effective_to is null or assignment.effective_to > now())
          and membership.status = cast(:#{#status.name()} as varchar)
          and organization.status = 'ACTIVE'
        union
        select assignment.parking_lot_id
        from people.employee_parking_lot_assignments assignment
        join people.employees employee on employee.employee_id = assignment.employee_id
        join iam.accounts account on account.user_profile_id = employee.user_profile_id
        join iam.organization_memberships membership
          on membership.organization_id = assignment.organization_id
         and membership.account_id = account.account_id
        join iam.organizations organization on organization.organization_id = assignment.organization_id
        where account.account_id = :accountId
          and assignment.status in ('ACTIVE', 'SCHEDULED')
          and assignment.effective_from <= now()
          and (assignment.effective_to is null or assignment.effective_to > now())
          and membership.status = cast(:#{#status.name()} as varchar)
          and organization.status = 'ACTIVE'
        """, nativeQuery = true)
    Set<UUID> findActiveParkingLotIdsByAccountId(
            @Param("accountId") UUID accountId,
            @Param("status") OrganizationMembershipStatus status
    );

    @Query(value = """
        select scoped.parking_lot_id from (
            select assignment.organization_id, assignment.parking_lot_id, assignment.manager_account_id as account_id
            from iam.parking_lot_management_assignments assignment
            where assignment.status = 'ACTIVE'
              and assignment.effective_from <= now()
              and (assignment.effective_to is null or assignment.effective_to > now())
            union all
            select assignment.organization_id, assignment.parking_lot_id, account.account_id
            from people.employee_parking_lot_assignments assignment
            join people.employees employee on employee.employee_id = assignment.employee_id
            join iam.accounts account on account.user_profile_id = employee.user_profile_id
            where assignment.status in ('ACTIVE', 'SCHEDULED')
              and assignment.effective_from <= now()
              and (assignment.effective_to is null or assignment.effective_to > now())
        ) scoped
        join iam.organization_memberships membership
          on membership.organization_id = scoped.organization_id
         and membership.account_id = scoped.account_id
        where scoped.organization_id = :organizationId
          and scoped.account_id = :accountId
          and membership.status = cast(:#{#status.name()} as varchar)
        """, nativeQuery = true)
    Set<UUID> findActiveParkingLotIdsByOrganizationIdAndAccountId(
            @Param("organizationId") UUID organizationId,
            @Param("accountId") UUID accountId,
            @Param("status") OrganizationMembershipStatus status
    );

    @Query(value = """
        select distinct assignment.manager_account_id
        from iam.parking_lot_management_assignments assignment
        join iam.accounts account on account.account_id = assignment.manager_account_id
        join iam.roles role on role.role_id = account.role_id
        join iam.organization_memberships membership
          on membership.organization_id = assignment.organization_id
         and membership.account_id = assignment.manager_account_id
        where assignment.parking_lot_id = :parkingLotId
          and assignment.status = 'ACTIVE'
          and assignment.effective_from <= now()
          and (assignment.effective_to is null or assignment.effective_to > now())
          and membership.status = cast(:#{#membershipStatus.name()} as varchar)
          and account.status = cast(:#{#accountStatus.name()} as varchar)
          and role.code = :roleCode
          and role.is_active = true
        """, nativeQuery = true)
    Set<UUID> findActiveAccountIdsByParkingLotId(
            @Param("parkingLotId") UUID parkingLotId,
            @Param("membershipStatus") OrganizationMembershipStatus membershipStatus,
            @Param("accountStatus") com.ban.vehicle_management.shared.enumeration.iam.AccountStatus accountStatus,
            @Param("roleCode") String roleCode
    );

    @Query(value = """
        select distinct scoped.account_id from (
            select assignment.organization_id, assignment.manager_account_id as account_id
            from iam.parking_lot_management_assignments assignment
            where assignment.parking_lot_id = :parkingLotId
              and assignment.status = 'ACTIVE'
              and assignment.effective_from <= now()
              and (assignment.effective_to is null or assignment.effective_to > now())
            union all
            select assignment.organization_id, account.account_id
            from people.employee_parking_lot_assignments assignment
            join people.employees employee on employee.employee_id = assignment.employee_id
            join iam.accounts account on account.user_profile_id = employee.user_profile_id
            where assignment.parking_lot_id = :parkingLotId
              and assignment.status in ('ACTIVE', 'SCHEDULED')
              and assignment.effective_from <= now()
              and (assignment.effective_to is null or assignment.effective_to > now())
        ) scoped
        join iam.organization_memberships membership
          on membership.organization_id = scoped.organization_id and membership.account_id = scoped.account_id
        join iam.organizations organization on organization.organization_id = scoped.organization_id
        join iam.accounts account on account.account_id = scoped.account_id
        join iam.roles role on role.role_id = account.role_id
        where membership.status = cast(:#{#membershipStatus.name()} as varchar)
          and organization.status = 'ACTIVE'
          and account.status = cast(:#{#accountStatus.name()} as varchar)
          and role.code in (:roleCodes)
          and role.is_active = true
        """, nativeQuery = true)
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
