package com.ban.vehicle_management.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PlatformFeePermissionSeedIT extends com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    PlatformFeePermissionSeedIT(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void shouldSeedSixPermissionsGrantedOnlyToSystemAdmin() {
        List<String> codes = List.of(
                "PLATFORM_FEE_CREATE_ALL",
                "PLATFORM_FEE_READ_ALL",
                "PLATFORM_FEE_UPDATE_ALL",
                "PLATFORM_FEE_ACTIVATE_ALL",
                "PLATFORM_FEE_ARCHIVE_ALL",
                "PLATFORM_FEE_PREVIEW_ALL");

        for (String code : codes) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM iam.permissions WHERE permission_code = ?", Integer.class, code);
            assertEquals(1, count, "Missing permission " + code);

            List<String> grantedRoles = jdbcTemplate.queryForList(
                    """
                    SELECT role.code FROM iam.role_permissions rp
                    JOIN iam.roles role ON role.role_id = rp.role_id
                    JOIN iam.permissions permission ON permission.permission_id = rp.permission_id
                    WHERE permission.permission_code = ? AND rp.is_active = true
                    """,
                    String.class, code);
            assertTrue(grantedRoles.contains("SYSTEM_ADMIN"), "SYSTEM_ADMIN must hold " + code);
            for (String role : grantedRoles) {
                assertEquals("SYSTEM_ADMIN", role, code + " must not be granted to " + role);
            }
        }
    }
}
