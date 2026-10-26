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
class PartnerAdminParkingSessionPermissionSeedIT
        extends com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    PartnerAdminParkingSessionPermissionSeedIT(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void shouldGrantPlateOperationPermissionsToPartnerAdmin() {
        List<String> actualCodes = jdbcTemplate.queryForList(
                """
                SELECT permission.permission_code
                FROM iam.role_permissions role_permission
                JOIN iam.roles role ON role.role_id = role_permission.role_id
                JOIN iam.permissions permission ON permission.permission_id = role_permission.permission_id
                WHERE role.code = 'PARTNER_ADMIN'
                  AND role_permission.is_active = true
                  AND permission.permission_code IN (
                      'PARKING_SESSION_PLATE_REVIEW_ALL',
                      'PARKING_SESSION_PLATE_OVERRIDE_ALL'
                  )
                ORDER BY permission.permission_code
                """,
                String.class);

        assertEquals(
                List.of(
                        "PARKING_SESSION_PLATE_OVERRIDE_ALL",
                        "PARKING_SESSION_PLATE_REVIEW_ALL"),
                actualCodes);
    }
}
