package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.domain.iam.account.model.CurrentAccessState;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.iam.account.model.RequiredAccessAction;
import com.ban.vehicle_management.domain.iam.account.policy.CurrentAccessStatePolicy;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InternalOnboardingEffectiveAccessPersistenceTest extends PostgresIntegrationTestSupport {

    @Autowired
    private AccountAuthorizationPersistenceAdapter authorizationAdapter;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProvisionedAccountPersistenceAdapter provisionedAccountAdapter;

    private final CurrentAccessStatePolicy statePolicy = new CurrentAccessStatePolicy();

    @Test
    void freshEmployeeThenPendingApprovalRemainOperationallyDenied() {
        UUID profileId = createProfile();
        UUID accountId = createAccount("EMPLOYEE", profileId);

        CurrentAccountAccess fresh = authorizationAdapter.findByAccountId(accountId).orElseThrow();
        var freshDecision = statePolicy.resolve(fresh);
        assertTrue(fresh.onboardingRequired());
        assertTrue(fresh.getEffectivePermissionCodes().isEmpty());
        assertEquals(CurrentAccessState.ONBOARDING_REQUIRED, freshDecision.state());
        assertEquals(RequiredAccessAction.COMPLETE_PROFILE, freshDecision.requiredAction());

        UUID employeeId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into people.employees (
                    employee_id, user_profile_id, employee_code, job_title, status, created_at
                ) values (?, ?, ?, 'Nhân viên vận hành', 'INACTIVE', now())
                """, employeeId, profileId, "EMP-" + employeeId);
        jdbcTemplate.update("""
                insert into operations.approval_requests (
                    approval_request_id, request_type, target_schema, target_table,
                    target_id, status, requested_by, request_data, created_at
                ) values (?, 'INTERNAL_EMPLOYEE_ONBOARDING', 'people', 'employees',
                          ?, 'PENDING', ?, '{}'::jsonb, now())
                """, UUID.randomUUID(), employeeId, accountId);

        CurrentAccountAccess pending = authorizationAdapter.findByAccountId(accountId).orElseThrow();
        var pendingDecision = statePolicy.resolve(pending);
        assertFalse(pending.onboardingRequired());
        assertEquals(ApprovalRequestStatus.PENDING, pending.onboardingApprovalStatus());
        assertTrue(pending.getEffectivePermissionCodes().isEmpty());
        assertEquals(CurrentAccessState.PENDING_APPROVAL, pendingDecision.state());
        assertEquals(RequiredAccessAction.WAIT_FOR_APPROVAL, pendingDecision.requiredAction());
        assertNotEquals(fresh.accessRevision(), pending.accessRevision());

        var lifecycle = accountRepository.findProvisionedAccountLifecycleByAccountIds(List.of(accountId));
        assertEquals(1, lifecycle.size());
        assertEquals("PENDING", lifecycle.getFirst().getInternalApprovalStatus());
    }

    @Test
    void approvedParkingManagerWithoutLotScopeHasNoOperationalPermission() {
        UUID profileId = createProfile();
        UUID accountId = createAccount("PARKING_MANAGER", profileId);
        UUID employeeId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into people.employees (
                    employee_id, user_profile_id, employee_code, job_title, status, created_at
                ) values (?, ?, ?, 'Quản lý bãi xe', 'ACTIVE', now())
                """, employeeId, profileId, "MGR-" + employeeId);
        jdbcTemplate.update("""
                insert into operations.approval_requests (
                    approval_request_id, request_type, target_schema, target_table,
                    target_id, status, requested_by, request_data, created_at
                ) values (?, 'INTERNAL_EMPLOYEE_ONBOARDING', 'people', 'employees',
                          ?, 'APPROVED', ?, '{}'::jsonb, now())
                """, UUID.randomUUID(), employeeId, accountId);
        UUID organizationId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into iam.organizations (organization_id, code, name, status, created_at)
                values (?, ?, 'Parking partner', 'ACTIVE', now())
                """, organizationId, "ORG-" + organizationId);
        jdbcTemplate.update("""
                insert into iam.organization_memberships (
                    organization_membership_id, organization_id, account_id, status, created_at
                ) values (?, ?, ?, 'ACTIVE', now())
                """, UUID.randomUUID(), organizationId, accountId);

        CurrentAccountAccess access = authorizationAdapter.findByAccountId(accountId).orElseThrow();
        var decision = statePolicy.resolve(access);
        assertFalse(access.onboardingRequired());
        assertTrue(access.permissionCodes().contains("PARKING_SCOPE_ASSIGNED"));
        assertTrue(access.assignedParkingLotIds().isEmpty());
        assertTrue(access.getEffectivePermissionCodes().isEmpty());
        assertEquals(CurrentAccessState.ORGANIZATION_ACCESS_REQUIRED, decision.state());
    }

    @Test
    void organizationMembershipDoesNotPretendUnsubmittedEmployeeWasApproved() {
        UUID profileId = createProfile();
        UUID accountId = createAccount("EMPLOYEE", profileId);
        UUID organizationId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into iam.organizations (organization_id, code, name, status, created_at)
                values (?, ?, 'Employee partner', 'ACTIVE', now())
                """, organizationId, "ORG-" + organizationId);
        jdbcTemplate.update("""
                insert into iam.organization_memberships (
                    organization_membership_id, organization_id, account_id, status, created_at
                ) values (?, ?, ?, 'ACTIVE', now())
                """, UUID.randomUUID(), organizationId, accountId);

        var account = provisionedAccountAdapter.findProvisionedAccountById(accountId).orElseThrow();

        assertEquals("PROFILE_REQUIRED", account.lifecycle().profileState());
        assertEquals("NOT_SUBMITTED", account.lifecycle().approvalState());
        assertEquals("ONBOARDING_REQUIRED", account.lifecycle().operationalAccessState());
        assertEquals(0, account.lifecycle().effectivePermissionCount());
    }

    private UUID createProfile() {
        UUID profileId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into people.user_profiles (
                    user_profile_id, full_name, status, created_at
                ) values (?, 'Onboarding user', 'ACTIVE', now())
                """, profileId);
        return profileId;
    }

    private UUID createAccount(String roleCode, UUID profileId) {
        UUID accountId = UUID.randomUUID();
        UUID roleId = jdbcTemplate.queryForObject(
                "select role_id from iam.roles where code = ?",
                UUID.class,
                roleCode);
        jdbcTemplate.update("""
                insert into iam.accounts (
                    account_id, user_profile_id, username, email, keycloak_user_id,
                    role_id, status, failed_login_count, created_at
                ) values (?, ?, ?, ?, ?, ?, ?, 0, now())
                """,
                accountId,
                profileId,
                "onboarding-" + accountId,
                "onboarding-" + accountId + "@example.com",
                "subject-" + accountId,
                roleId,
                AccountStatus.ACTIVE.name());
        return accountId;
    }
}
