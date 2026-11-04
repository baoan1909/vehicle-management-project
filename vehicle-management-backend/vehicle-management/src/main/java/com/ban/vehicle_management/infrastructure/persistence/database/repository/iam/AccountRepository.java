package com.ban.vehicle_management.infrastructure.persistence.database.repository.iam;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;

import java.util.Collection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID>, JpaSpecificationExecutor<AccountEntity> {
    interface ProvisionedAccountLifecycleProjection {
        UUID getAccountId();
        String getEmployeeStatus();
        String getCustomerStatus();
        String getCustomerApprovalStatus();
        String getInternalApprovalStatus();
        String getPartnerApprovalStatus();
        Boolean getHasActiveOrganizationMembership();
        Boolean getHasActiveParkingLotScope();
    }

    boolean existsByRoleId(UUID roleId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByAccountIdAndStatusNot(UUID accountId, AccountStatus status);

    Optional<AccountEntity> findByKeycloakUserId(String keycloakUserId);

    Optional<AccountEntity> findByUserProfileId(UUID userProfileId);

    Optional<AccountEntity> findByUsername(String username);

    Optional<AccountEntity> findByEmail(String email);

    @Query(value = """
        SELECT account.account_id AS "accountId",
               CAST(employee.status AS varchar) AS "employeeStatus",
               CAST(customer.status AS varchar) AS "customerStatus",
               CAST(customer.approval_status AS varchar) AS "customerApprovalStatus",
               (
                   SELECT CAST(approval.status AS varchar)
                   FROM operations.approval_requests approval
                   WHERE approval.request_type = 'INTERNAL_EMPLOYEE_ONBOARDING'
                     AND approval.target_schema = 'people'
                     AND approval.target_table = 'employees'
                     AND approval.target_id = employee.employee_id
                   ORDER BY approval.created_at DESC, approval.approval_request_id DESC
                   LIMIT 1
               ) AS "internalApprovalStatus",
               (
                   SELECT CAST(approval.status AS varchar)
                   FROM operations.approval_requests approval
                   WHERE approval.request_type = 'PARTNER_REGISTRATION'
                     AND approval.target_schema = 'iam'
                     AND approval.target_table = 'accounts'
                     AND approval.requested_by = account.account_id
                   ORDER BY approval.created_at DESC, approval.approval_request_id DESC
                   LIMIT 1
               ) AS "partnerApprovalStatus",
               EXISTS (
                   SELECT 1
                   FROM iam.organization_memberships membership
                   JOIN iam.organizations organization
                     ON organization.organization_id = membership.organization_id
                   WHERE membership.account_id = account.account_id
                     AND membership.status = 'ACTIVE'
                     AND organization.status = 'ACTIVE'
               ) AS "hasActiveOrganizationMembership",
               (
                   EXISTS (
                       SELECT 1
                       FROM iam.parking_lot_management_assignments assignment
                       JOIN iam.organization_memberships membership
                         ON membership.organization_id = assignment.organization_id
                        AND membership.account_id = assignment.manager_account_id
                       JOIN iam.organizations organization
                         ON organization.organization_id = assignment.organization_id
                       WHERE assignment.manager_account_id = account.account_id
                         AND assignment.status = 'ACTIVE'
                         AND assignment.effective_from <= now()
                         AND (assignment.effective_to IS NULL OR assignment.effective_to > now())
                         AND membership.status = 'ACTIVE'
                         AND organization.status = 'ACTIVE'
                   ) OR EXISTS (
                       SELECT 1
                       FROM people.employee_parking_lot_assignments assignment
                       JOIN people.employees scoped_employee
                         ON scoped_employee.employee_id = assignment.employee_id
                       JOIN iam.organization_memberships membership
                         ON membership.organization_id = assignment.organization_id
                        AND membership.account_id = account.account_id
                       JOIN iam.organizations organization
                         ON organization.organization_id = assignment.organization_id
                       WHERE scoped_employee.user_profile_id = account.user_profile_id
                         AND assignment.status IN ('ACTIVE', 'SCHEDULED')
                         AND assignment.effective_from <= now()
                         AND (assignment.effective_to IS NULL OR assignment.effective_to > now())
                         AND membership.status = 'ACTIVE'
                         AND organization.status = 'ACTIVE'
                   )
               ) AS "hasActiveParkingLotScope"
        FROM iam.accounts account
        LEFT JOIN people.employees employee
          ON employee.user_profile_id = account.user_profile_id
        LEFT JOIN people.customers customer
          ON customer.user_profile_id = account.user_profile_id
        WHERE account.account_id IN (:accountIds)
        """, nativeQuery = true)
    List<ProvisionedAccountLifecycleProjection> findProvisionedAccountLifecycleByAccountIds(
            @Param("accountIds") Collection<UUID> accountIds
    );


    @Query("""
        select case when count(accountEntity) > 0 then true else false end
        from AccountEntity accountEntity
        join accountEntity.role roleEntity
        where accountEntity.accountId = :accountId
          and accountEntity.status = :status
          and roleEntity.code in :roleCodes
          and roleEntity.isActive = true
        """)
    boolean existsAssignableSupportTicketAccount(
            @Param("accountId") UUID accountId,
            @Param("status") AccountStatus status,
            @Param("roleCodes") Collection<String> roleCodes
    );

    @Query("""
        select distinct accountEntity.accountId
        from AccountEntity accountEntity
        join accountEntity.role roleEntity
        where accountEntity.status = :status
          and (
                :allActiveAccounts = true
                or accountEntity.accountId in :accountIds
                or roleEntity.code in :roleCodes
          )
        order by accountEntity.accountId
        """)
    List<UUID> findActiveAccountIdsForBroadcast(
            @Param("status") AccountStatus status,
            @Param("allActiveAccounts") boolean allActiveAccounts,
            @Param("roleCodes") Collection<String> roleCodes,
            @Param("accountIds") Collection<UUID> accountIds
    );

}


