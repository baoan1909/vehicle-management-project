package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.domain.iam.account.model.AccountProfileState;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationMembershipStatus;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus;
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
class PartnerAdminEffectiveAccessPersistenceIT extends PostgresIntegrationTestSupport {

    private static final UUID SYSTEM_ADMIN_ROLE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PARTNER_ADMIN_ROLE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000006");

    @Autowired
    private AccountAuthorizationPersistenceAdapter authorizationAdapter;

    @Autowired
    private AccountProfilePersistenceAdapter profileAdapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void activePartnerWithActiveMembershipInActiveOrganizationShouldUsePermissions() {
        UUID accountId = createAccount("PARTNER_ADMIN", AccountStatus.ACTIVE);
        UUID organizationId = createOrganization(OrganizationStatus.ACTIVE);
        createMembership(organizationId, accountId, OrganizationMembershipStatus.ACTIVE);

        assertEffectiveAccess(accountId, true, true, true);
        assertEquals(0L, countPartnerApprovalRequests(accountId));
    }

    @Test
    void activePartnerWithoutMembershipShouldNotUsePermissions() {
        UUID accountId = createAccount("PARTNER_ADMIN", AccountStatus.ACTIVE);

        assertEffectiveAccess(accountId, true, false, false);
    }

    @Test
    void activePartnerWithMembershipInSuspendedOrganizationShouldNotUsePermissions() {
        UUID accountId = createAccount("PARTNER_ADMIN", AccountStatus.ACTIVE);
        UUID organizationId = createOrganization(OrganizationStatus.SUSPENDED);
        createMembership(organizationId, accountId, OrganizationMembershipStatus.ACTIVE);

        assertEffectiveAccess(accountId, true, false, false);
    }

    @Test
    void activePartnerWithInactiveMembershipShouldNotUsePermissions() {
        UUID accountId = createAccount("PARTNER_ADMIN", AccountStatus.ACTIVE);
        UUID organizationId = createOrganization(OrganizationStatus.ACTIVE);
        createMembership(organizationId, accountId, OrganizationMembershipStatus.INACTIVE);

        assertEffectiveAccess(accountId, true, false, false);
    }

    @Test
    void activePartnerWithOneEligibleMembershipShouldUsePermissions() {
        UUID accountId = createAccount("PARTNER_ADMIN", AccountStatus.ACTIVE);
        UUID inactiveOrganizationId = createOrganization(OrganizationStatus.SUSPENDED);
        UUID activeOrganizationId = createOrganization(OrganizationStatus.ACTIVE);
        createMembership(inactiveOrganizationId, accountId, OrganizationMembershipStatus.ACTIVE);
        createMembership(activeOrganizationId, accountId, OrganizationMembershipStatus.ACTIVE);

        assertEffectiveAccess(accountId, true, true, true);
    }

    @Test
    void pendingPartnerWithActiveMembershipShouldNotUsePermissions() {
        UUID accountId = createAccount("PARTNER_ADMIN", AccountStatus.PENDING);
        UUID organizationId = createOrganization(OrganizationStatus.ACTIVE);
        createMembership(organizationId, accountId, OrganizationMembershipStatus.ACTIVE);

        assertEffectiveAccess(accountId, false, false, true);
    }

    @Test
    void activePartnerWithInactiveRoleShouldNotUsePermissions() {
        jdbcTemplate.update(
                "update iam.roles set is_active = false where role_id = ?",
                PARTNER_ADMIN_ROLE_ID
        );
        UUID accountId = createAccount("PARTNER_ADMIN", AccountStatus.ACTIVE);
        UUID organizationId = createOrganization(OrganizationStatus.ACTIVE);
        createMembership(organizationId, accountId, OrganizationMembershipStatus.ACTIVE);

        assertEffectiveAccess(accountId, false, false, false);
    }

    @Test
    void systemAdminStillUsesPermissionsWithoutOrganizationMembership() {
        UUID accountId = createAccount("SYSTEM_ADMIN", AccountStatus.ACTIVE);

        assertEffectiveAccess(accountId, true, false, false);
    }

    private void assertEffectiveAccess(
            UUID accountId,
            boolean expectedCommonGate,
            boolean expectedPartnerScope,
            boolean expectedActivePartnerMembership
    ) {
        CurrentAccountAccess currentAccess = authorizationAdapter.findByAccountId(accountId)
                .orElseThrow();
        AccountProfileState onboarding = profileAdapter.findProfileStateByAccountId(accountId)
                .orElseThrow();

        assertEquals(expectedCommonGate, currentAccess.canUseBusinessPermissions());
        assertEquals(
                expectedActivePartnerMembership,
                currentAccess.hasActivePartnerOrganizationMembership()
        );
        assertEquals(
                List.copyOf(currentAccess.getEffectivePermissionCodes()),
                onboarding.permissionCodes()
        );
        assertEquals(expectedPartnerScope,
                currentAccess.getEffectivePermissionCodes().contains("PARKING_SCOPE_PARTNER"));
        if (expectedCommonGate) {
            assertTrue(onboarding.permissionCodes().contains("ORGANIZATION_READ_ALL"));
        }
    }

    private UUID createAccount(String roleCode, AccountStatus status) {
        UUID accountId = UUID.randomUUID();
        UUID roleId = "PARTNER_ADMIN".equals(roleCode) ? PARTNER_ADMIN_ROLE_ID : SYSTEM_ADMIN_ROLE_ID;
        jdbcTemplate.update(
                """
                insert into iam.accounts (
                    account_id, username, email, keycloak_user_id, role_id,
                    status, failed_login_count, created_at
                ) values (?, ?, ?, ?, ?, ?, 0, now())
                """,
                accountId,
                "account-" + accountId,
                "account-" + accountId + "@example.com",
                "subject-" + accountId,
                roleId,
                status.name()
        );
        return accountId;
    }

    private UUID createOrganization(OrganizationStatus status) {
        UUID organizationId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                insert into iam.organizations (
                    organization_id, code, name, status, created_at
                ) values (?, ?, ?, ?, now())
                """,
                organizationId,
                "org-" + organizationId,
                "Organization " + organizationId,
                status.name()
        );
        return organizationId;
    }

    private void createMembership(
            UUID organizationId,
            UUID accountId,
            OrganizationMembershipStatus status
    ) {
        jdbcTemplate.update(
                """
                insert into iam.organization_memberships (
                    organization_membership_id, organization_id, account_id, status, created_at
                ) values (?, ?, ?, ?, now())
                """,
                UUID.randomUUID(),
                organizationId,
                accountId,
                status.name()
        );
    }

    private long countPartnerApprovalRequests(UUID accountId) {
        Long count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from operations.approval_requests
                where requested_by = ?
                  and request_type = 'PARTNER_REGISTRATION'
                """,
                Long.class,
                accountId
        );
        return count == null ? 0L : count;
    }
}
