package com.ban.vehicle_management.infrastructure.security.authorization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.infrastructure.persistence.adapter.iam.AccountAuthorizationPersistenceAdapter;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityManager;
import java.util.Set;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@Transactional
class AuthorizationActorMatrixSqlStatementCountIT extends PostgresIntegrationTestSupport {

    private static final UUID SYSTEM_ADMIN_ROLE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID EMPLOYEE_ROLE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired EntityManager entityManager;
    @Autowired AccountAuthorizationPersistenceAdapter authorizationAdapter;

    private Statistics statistics;

    @BeforeEach
    void prepareStatistics() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
    }

    @Test
    void activeSystemAdminUsesOneFiveStatementAggregateLoad() {
        UUID accountId = createAccount(SYSTEM_ADMIN_ROLE_ID, AccountStatus.ACTIVE, createProfile());
        assertLoad(accountId, 5L);
    }

    @Test
    void partnerWithOrWithoutEligibleMembershipUsesOneSixStatementAggregateLoad() {
        UUID roleId = createRole("QUERY_PARTNER", true, Set.of("PARKING_SCOPE_PARTNER"));
        UUID eligibleAccount = createAccount(roleId, AccountStatus.ACTIVE, createProfile());
        UUID organizationId = createOrganization("ACTIVE");
        createMembership(organizationId, eligibleAccount, "ACTIVE");
        assertTrue(assertLoad(eligibleAccount, 6L).hasActivePartnerOrganizationMembership());

        UUID accountWithoutMembership = createAccount(roleId, AccountStatus.ACTIVE, createProfile());
        assertTrue(!assertLoad(accountWithoutMembership, 6L).hasActivePartnerOrganizationMembership());
    }

    @Test
    void assignedEmployeeUsesOneSevenStatementAggregateLoad() {
        UUID profileId = createProfile();
        UUID employeeId = createEmployee(profileId, "ACTIVE");
        UUID accountId = createAccount(EMPLOYEE_ROLE_ID, AccountStatus.ACTIVE, profileId);
        UUID organizationId = createOrganization("ACTIVE");
        createMembership(organizationId, accountId, "ACTIVE");
        UUID parkingLotId = createParkingLot(organizationId);
        jdbcTemplate.update(
                "insert into people.employee_parking_lot_assignments "
                        + "(assignment_id, organization_id, parking_lot_id, employee_id, assignment_type, "
                        + "status, effective_from, assigned_by_account_id, assignment_source, created_at) "
                        + "values (?, ?, ?, ?, 'PRIMARY', 'ACTIVE', now(), ?, 'MANAGER', now())",
                UUID.randomUUID(), organizationId, parkingLotId, employeeId, accountId
        );

        CurrentAccountAccess access = assertLoad(accountId, 7L);
        assertEquals(Set.of(parkingLotId), access.assignedParkingLotIds());
    }

    @Test
    void approvedCustomerUsesOneFiveStatementAggregateLoad() {
        UUID roleId = createRole("QUERY_CUSTOMER", true, Set.of("CUSTOMER_VEHICLE_READ_OWN"));
        UUID profileId = createProfile();
        createCustomer(profileId, "ACTIVE", "APPROVED");
        UUID accountId = createAccount(roleId, AccountStatus.ACTIVE, profileId);

        CurrentAccountAccess access = assertLoad(accountId, 5L);
        assertTrue(access.getEffectivePermissionCodes().contains("CUSTOMER_VEHICLE_READ_OWN"));
    }

    @Test
    void pendingCustomerUsesOneFiveStatementAggregateLoadWithSupportOnly() {
        UUID roleId = createRole("QUERY_PENDING_CUSTOMER", true,
                Set.of("SUPPORT_TICKET_READ_OWN", "CUSTOMER_VEHICLE_READ_OWN"));
        UUID profileId = createProfile();
        createCustomer(profileId, "INACTIVE", "PENDING");
        UUID accountId = createAccount(roleId, AccountStatus.PENDING, profileId);

        CurrentAccountAccess access = assertLoad(accountId, 5L);

        assertEquals(Set.of("SUPPORT_TICKET_READ_OWN"), access.getEffectivePermissionCodes());
        assertTrue(access.canUseEffectivePermissions());
        assertTrue(!access.canUseBusinessPermissions());
    }

    @Test
    void rejectedCustomerUsesOneFiveStatementAggregateLoadWithSupportOnly() {
        UUID roleId = createRole("QUERY_REJECTED_CUSTOMER", true,
                Set.of("CHAT_CONVERSATION_READ_OWN", "WALLET_READ_OWN"));
        UUID profileId = createProfile();
        createCustomer(profileId, "INACTIVE", "REJECTED");
        UUID accountId = createAccount(roleId, AccountStatus.PENDING, profileId);

        CurrentAccountAccess access = assertLoad(accountId, 5L);

        assertEquals(Set.of("CHAT_CONVERSATION_READ_OWN"), access.getEffectivePermissionCodes());
        assertTrue(access.canUseEffectivePermissions());
        assertTrue(!access.canUseBusinessPermissions());
    }

    @Test
    void customRoleLockedAccountAndInactiveRoleEachUseOneFiveStatementAggregateLoad() {
        UUID customRoleId = createRole("QUERY_CUSTOM", true, Set.of("ACCOUNT_READ_ALL"));
        UUID customAccount = createAccount(customRoleId, AccountStatus.ACTIVE, createProfile());
        assertTrue(assertLoad(customAccount, 5L).getEffectivePermissionCodes().contains("ACCOUNT_READ_ALL"));

        UUID lockedAccount = createAccount(customRoleId, AccountStatus.LOCKED, createProfile());
        assertTrue(assertLoad(lockedAccount, 5L).getEffectivePermissionCodes().isEmpty());

        UUID inactiveRoleId = createRole("QUERY_INACTIVE", false, Set.of("ACCOUNT_READ_ALL"));
        UUID inactiveRoleAccount = createAccount(inactiveRoleId, AccountStatus.ACTIVE, createProfile());
        assertTrue(assertLoad(inactiveRoleAccount, 5L).getEffectivePermissionCodes().isEmpty());
    }

    private CurrentAccountAccess assertLoad(UUID accountId, long expectedStatements) {
        entityManager.clear();
        statistics.clear();
        CurrentAccountAccess access = authorizationAdapter.findByAccountId(accountId).orElseThrow();
        assertEquals(expectedStatements, statistics.getPrepareStatementCount());
        return access;
    }

    private UUID createRole(String code, boolean active, Set<String> permissionCodes) {
        UUID roleId = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into iam.roles (role_id, code, name, is_system, is_active, created_at) "
                        + "values (?, ?, ?, false, ?, now())",
                roleId, code, code, active
        );
        permissionCodes.forEach(permissionCode -> jdbcTemplate.update(
                """
                insert into iam.role_permissions (id, role_id, permission_id, created_at, is_active, is_system)
                select ?, ?, permission_id, now(), true, false
                from iam.permissions where permission_code = ?
                """,
                UUID.randomUUID(), roleId, permissionCode
        ));
        return roleId;
    }

    private UUID createAccount(UUID roleId, AccountStatus status, UUID profileId) {
        UUID accountId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                insert into iam.accounts (
                    account_id, user_profile_id, username, email, keycloak_user_id, role_id,
                    status, failed_login_count, created_at
                ) values (?, ?, ?, ?, ?, ?, ?, 0, now())
                """,
                accountId, profileId, "query-" + accountId, "query-" + accountId + "@example.com",
                "subject-" + accountId, roleId, status.name()
        );
        return accountId;
    }

    private UUID createProfile() {
        UUID profileId = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into people.user_profiles (user_profile_id, full_name, status, created_at) "
                        + "values (?, ?, 'ACTIVE', now())",
                profileId, "Query Count Profile"
        );
        return profileId;
    }

    private UUID createEmployee(UUID profileId, String status) {
        UUID employeeId = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into people.employees "
                        + "(employee_id, user_profile_id, employee_code, status, created_at) "
                        + "values (?, ?, ?, ?, now())",
                employeeId, profileId, "EMP-" + UUID.randomUUID(), status
        );
        return employeeId;
    }

    private void createCustomer(UUID profileId, String status, String approvalStatus) {
        jdbcTemplate.update(
                "insert into people.customers "
                        + "(customer_id, user_profile_id, customer_code, customer_type, status, approval_status, created_at) "
                        + "values (?, ?, ?, 'REGISTERED', ?, ?, now())",
                UUID.randomUUID(), profileId, "CUS-" + UUID.randomUUID(), status, approvalStatus
        );
    }

    private UUID createOrganization(String status) {
        UUID organizationId = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into iam.organizations (organization_id, code, name, status, created_at) "
                        + "values (?, ?, ?, ?, now())",
                organizationId, "ORG-" + organizationId, "Query Organization", status
        );
        return organizationId;
    }

    private UUID createMembership(UUID organizationId, UUID accountId, String status) {
        UUID membershipId = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into iam.organization_memberships "
                        + "(organization_membership_id, organization_id, account_id, status, created_at) "
                        + "values (?, ?, ?, ?, now())",
                membershipId, organizationId, accountId, status
        );
        return membershipId;
    }

    private UUID createParkingLot(UUID organizationId) {
        UUID parkingLotId = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into parking.parking_lots "
                        + "(parking_lot_id, organization_id, code, name, total_capacity, status, created_at) "
                        + "values (?, ?, ?, ?, 0, 'SETUP', now())",
                parkingLotId, organizationId, "LOT-" + parkingLotId, "Query Parking Lot"
        );
        return parkingLotId;
    }
}
