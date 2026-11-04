package com.ban.vehicle_management.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport;
import java.util.List;
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
    }
}
