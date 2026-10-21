package com.ban.vehicle_management.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class UserGuidePermissionSeedIT extends com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    UserGuidePermissionSeedIT(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void shouldSeedFivePermissionsGrantedOnlyToSystemAdmin() {
        List<String> expectedCodes = List.of(
                "USER_GUIDE_CREATE_ALL",
                "USER_GUIDE_READ_ALL",
                "USER_GUIDE_UPDATE_ALL",
                "USER_GUIDE_PUBLISH_ALL",
                "USER_GUIDE_ARCHIVE_ALL");

        List<String> actualCodes = jdbcTemplate.queryForList(
                """
                SELECT permission.permission_code
                FROM iam.permissions permission
                JOIN iam.permission_modules module ON module.module_id = permission.module_id
                WHERE module.code = 'USER_GUIDE'
                ORDER BY permission.permission_code
                """,
                String.class);
        assertEquals(expectedCodes.stream().sorted().toList(), actualCodes);

        for (String permissionCode : expectedCodes) {
            List<String> activeRoles = jdbcTemplate.queryForList(
                    """
                    SELECT role.code
                    FROM iam.role_permissions role_permission
                    JOIN iam.roles role ON role.role_id = role_permission.role_id
                    JOIN iam.permissions permission ON permission.permission_id = role_permission.permission_id
                    WHERE permission.permission_code = ?
                      AND role_permission.is_active = true
                    ORDER BY role.code
                    """,
                    String.class,
                    permissionCode);
            assertEquals(List.of("SYSTEM_ADMIN"), activeRoles, permissionCode);
        }
    }
}
