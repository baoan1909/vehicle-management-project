package com.ban.vehicle_management.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.LostCardReportRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.OrganizationMembershipRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AccountDeletionMigrationIT extends PostgresIntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrganizationMembershipRepository organizationMembershipRepository;

    @Autowired
    private LostCardReportRepository lostCardReportRepository;

    @Test
    void migrationCreatesPostgresLifecycleIndexesAndPermissionVocabulary() {
        Integer tableCount = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema='iam' and table_name='account_deletion_requests'
                """, Integer.class);
        assertEquals(1, tableCount);

        String partialIndex = jdbcTemplate.queryForObject("""
                select indexdef from pg_indexes
                where schemaname='iam' and indexname='uq_account_deletion_active_request'
                """, String.class);
        assertTrue(partialIndex.contains("WHERE"));
        assertTrue(partialIndex.contains("GRACE_PERIOD"));

        List<String> permissions = jdbcTemplate.queryForList("""
                select permission_code from iam.permissions
                where permission_code like 'ACCOUNT_DELETION_%_SELF'
                order by permission_code
                """, String.class);
        assertEquals(List.of(
                "ACCOUNT_DELETION_CANCEL_SELF",
                "ACCOUNT_DELETION_READ_SELF",
                "ACCOUNT_DELETION_REQUEST_SELF"
        ), permissions);

        Integer markerCapacity = jdbcTemplate.queryForObject("""
                select character_maximum_length from information_schema.columns
                where table_schema='people' and table_name='user_profiles' and column_name='phone_number'
                """, Integer.class);
        assertTrue(markerCapacity >= 64);

        Integer previousStatusColumn = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.columns
                where table_schema='iam' and table_name='account_deletion_requests'
                  and column_name='previous_account_status' and is_nullable='NO'
                """, Integer.class);
        assertEquals(1, previousStatusColumn);

        String accountStatusConstraint = jdbcTemplate.queryForObject("""
                select pg_get_constraintdef(oid) from pg_constraint
                where conname='ck_accounts_status' and conrelid='iam.accounts'::regclass
                """, String.class);
        assertTrue(accountStatusConstraint.contains("PENDING_DELETION"));
        assertTrue(accountStatusConstraint.contains("DELETED"));

        String employeeStatusConstraint = jdbcTemplate.queryForObject("""
                select pg_get_constraintdef(oid) from pg_constraint
                where conname='ck_employees_status' and conrelid='people.employees'::regclass
                """, String.class);
        assertTrue(employeeStatusConstraint.contains("DELETED"));

        Integer internalRoleGrantCount = jdbcTemplate.queryForObject("""
                select count(*) from iam.role_permissions grant_row
                join iam.roles role on role.role_id=grant_row.role_id
                join iam.permissions permission on permission.permission_id=grant_row.permission_id
                where role.code in ('PARTNER_ADMIN','PARKING_MANAGER','EMPLOYEE')
                  and permission.permission_code like 'ACCOUNT_DELETION_%_SELF'
                  and grant_row.is_active=true
                """, Integer.class);
        assertEquals(9, internalRoleGrantCount);
    }

    @Test
    void lastPermissionHolderQueryUsesPostgresSafeAliases() {
        assertTrue(organizationMembershipRepository
                .findOrganizationsWhereAccountIsLastPermissionHolder(
                        UUID.randomUUID(), "ORGANIZATION_CLOSE_OWN")
                .isEmpty());
    }

    @Test
    void accountDeletionLostCardLookupDoesNotBindNullableDateParameters() {
        assertTrue(lostCardReportRepository
                .findAllByCustomerIdOrderByNotificationTimeDesc(UUID.randomUUID())
                .isEmpty());
    }

    @Test
    void lostCardFilterAcceptsTypedDefaultDateBounds() {
        assertTrue(lostCardReportRepository.findBusinessVisibleWithFilters(
                null, null, UUID.randomUUID(), null, null, null,
                Instant.parse("1900-01-01T00:00:00Z"),
                Instant.parse("9999-12-31T23:59:59Z"),
                "", false, Set.of(new UUID(0L, 0L)))
                .isEmpty());
    }
}
