package com.ban.vehicle_management.infrastructure.security.authorization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.infrastructure.security.adapter.CurrentAccountSecurityAdapter;
import com.ban.vehicle_management.infrastructure.security.jwt.JwtAuthenticationConverter;
import com.ban.vehicle_management.infrastructure.security.principal.AuthenticatedAccountPrincipal;
import com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuthorizationSqlStatementCountIT.SnapshotProbeController.class)
class AuthorizationSqlStatementCountIT extends PostgresIntegrationTestSupport {

    private static final UUID SYSTEM_ADMIN_ROLE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Autowired
    private CurrentAccountSecurityAdapter currentAccountSecurityAdapter;

    @Autowired
    private PermissionAuthorizer permissionAuthorizer;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private UUID accountId;
    private Statistics statistics;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                insert into iam.accounts (
                    account_id, username, email, keycloak_user_id, role_id,
                    status, failed_login_count, created_at
                ) values (?, ?, ?, ?, ?, 'ACTIVE', 0, now())
                """,
                accountId,
                "query-count-" + accountId,
                "query-count-" + accountId + "@example.com",
                "subject-" + accountId,
                SYSTEM_ADMIN_ROLE_ID
        );
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        jdbcTemplate.update("delete from iam.accounts where account_id = ?", accountId);
    }

    @Test
    void jwtThenCurrentAccessExecutesOneAggregateLoad() {
        SecurityContextHolder.getContext().setAuthentication(
                jwtAuthenticationConverter.convert(jwt(accountId))
        );

        currentAccountSecurityAdapter.getCurrentAccountOrThrow();

        assertEquals(3L, statistics.getPrepareStatementCount());
    }

    @Test
    void preAuthorizeThenTwoUseCaseReadsExecuteOneAggregateLoad() {
        bindLegacyAuthentication();

        permissionAuthorizer.hasPermission("ACCOUNT_READ_ALL");
        currentAccountSecurityAdapter.getCurrentAccountOrThrow();
        currentAccountSecurityAdapter.getCurrentAccountOrThrow();

        assertEquals(3L, statistics.getPrepareStatementCount());
    }

    @Test
    void hasAnyPermissionExecutesOneAggregateLoad() {
        bindLegacyAuthentication();

        permissionAuthorizer.hasAnyPermission("MISSING_A", "MISSING_B", "ACCOUNT_READ_ALL");

        assertEquals(3L, statistics.getPrepareStatementCount());
    }

    @Test
    void realBearerRequestReusesJwtSnapshotAcrossPreAuthorizeControllerAndUseCaseReads() throws Exception {
        when(jwtDecoder.decode("query-token")).thenReturn(jwt(accountId));

        mockMvc.perform(get("/api/test/access-snapshot")
                        .header("Authorization", "Bearer query-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId.toString()));

        assertEquals(3L, statistics.getPrepareStatementCount());
    }

    private void bindLegacyAuthentication() {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "legacy-user",
                "credentials",
                List.of()
        );
        authentication.setDetails(new AuthenticatedAccountPrincipal(
                accountId,
                "subject-" + accountId,
                "query-count-" + accountId,
                "query-count-" + accountId + "@example.com"
        ));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Jwt jwt(UUID id) {
        Instant issuedAt = Instant.now();
        return new Jwt(
                "token",
                issuedAt,
                issuedAt.plusSeconds(300),
                Map.of("alg", "none"),
                Map.of(
                        "sub", "subject-" + id,
                        "account_id", id.toString(),
                        "preferred_username", "query-count-" + id
                )
        );
    }

    @RestController
    static class SnapshotProbeController {
        private final CurrentAccountPortIn currentAccountPortIn;

        SnapshotProbeController(CurrentAccountPortIn currentAccountPortIn) {
            this.currentAccountPortIn = currentAccountPortIn;
        }

        @GetMapping("/api/test/access-snapshot")
        @PreAuthorize("@permissionAuthorizer.hasPermission('ACCOUNT_READ_ALL')")
        Map<String, String> probe() {
            UUID firstRead = currentAccountPortIn.getCurrentAccountIdOrThrow();
            UUID secondRead = currentAccountPortIn.getCurrentAccountOrThrow().accountId();
            currentAccountPortIn.requirePermission("ACCOUNT_READ_ALL");
            return Map.of("accountId", firstRead.toString(), "sameSnapshot", Boolean.toString(firstRead.equals(secondRead)));
        }
    }
}
