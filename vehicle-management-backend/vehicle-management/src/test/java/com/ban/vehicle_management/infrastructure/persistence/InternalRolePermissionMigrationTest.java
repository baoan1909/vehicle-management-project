package com.ban.vehicle_management.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class InternalRolePermissionMigrationTest {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse(
            System.getenv().getOrDefault("POSTGRES_SPATIAL_TEST_IMAGE",
                    "vehicle-management/postgres-pgvector-postgis:17"))
            .asCompatibleSubstituteFor("postgres"));

    @Test
    void migratesAndReplaysScopedPermissionsWithoutGlobalBroadcastForOtherRoles() throws Exception {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .load().migrate();
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            String migration = Files.readString(Path.of(
                    "src/main/resources/db/migration/V20261128120000__harden_internal_role_permissions.sql"));
            for (int pass = 0; pass < 2; pass++) {
                statement.execute(migration);
                try (var result = statement.executeQuery("""
                        select count(*) from iam.role_permissions rp
                        join iam.roles r on r.role_id = rp.role_id
                        join iam.permissions p on p.permission_id = rp.permission_id
                        where rp.is_active and r.code <> 'SYSTEM_ADMIN'
                          and p.permission_code like 'BROADCAST_NOTIFICATION_%'
                        """)) {
                    result.next();
                    assertEquals(0, result.getInt(1));
                }
                try (var result = statement.executeQuery("""
                        select count(*) from iam.role_permissions rp
                        join iam.roles r on r.role_id = rp.role_id
                        join iam.permissions p on p.permission_id = rp.permission_id
                        where rp.is_active and r.code in ('SYSTEM_ADMIN','PARTNER_ADMIN','PARKING_MANAGER')
                          and p.permission_code in ('AVATAR_APPROVAL_READ_ALL','AVATAR_APPROVAL_REVIEW_ALL')
                        """)) {
                    result.next();
                    assertEquals(6, result.getInt(1));
                }
                try (var result = statement.executeQuery("""
                        select count(*) from iam.role_permissions rp
                        join iam.roles r on r.role_id = rp.role_id
                        join iam.permissions p on p.permission_id = rp.permission_id
                        where rp.is_active and r.code = 'SYSTEM_ADMIN'
                          and p.permission_code like 'BROADCAST_NOTIFICATION_%'
                        """)) {
                    result.next();
                    assertEquals(6, result.getInt(1));
                }
            }
        }
    }
}
