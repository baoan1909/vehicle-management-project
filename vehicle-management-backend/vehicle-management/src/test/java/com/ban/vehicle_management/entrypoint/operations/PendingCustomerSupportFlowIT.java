package com.ban.vehicle_management.entrypoint.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PendingCustomerSupportFlowIT extends PostgresIntegrationTestSupport {

    private static final Set<String> SUPPORT_PERMISSIONS = Set.of(
            "SUPPORT_WIDGET_ACCESS_OWN",
            "SUPPORT_TICKET_CREATE_OWN",
            "SUPPORT_TICKET_READ_OWN",
            "CHAT_CONVERSATION_READ_OWN",
            "CHAT_MESSAGE_SEND_OWN");

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired ObjectMapper objectMapper;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired EntityManager entityManager;
    @MockitoBean JwtDecoder jwtDecoder;
    private Statistics statistics;

    @BeforeEach
    void setUp() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
    }

    @Test
    void pendingCustomerCanListCreateOpenAssistantReadAndSendWithSupportOnlyPermissions() throws Exception {
        Actor actor = createActor("PENDING", true, SUPPORT_PERMISSIONS);
        bindToken("pending-support-token", actor.accountId());

        entityManager.clear();
        statistics.clear();
        mockMvc.perform(get("/api/operations/support-tickets/mine")
                        .header("Authorization", "Bearer pending-support-token"))
                .andExpect(status().isOk());
        assertEquals(7L, statistics.getPrepareStatementCount(),
                "current-access aggregate (5) + narrow customer lookup (1) + ticket query (1)");

        UUID categoryId = jdbcTemplate.queryForObject(
                "select category_id from operations.support_ticket_categories where status = 'ACTIVE' limit 1",
                UUID.class);
        String createBody = objectMapper.writeValueAsString(Map.of(
                "categoryId", categoryId,
                "title", "Pending customer support",
                "content", "Please help with onboarding"));
        entityManager.clear();
        statistics.clear();
        mockMvc.perform(post("/api/operations/support-tickets")
                        .header("Authorization", "Bearer pending-support-token")
                        .header("Idempotency-Key", "pending-" + actor.accountId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated());
        long createStatements = statistics.getPrepareStatementCount();

        entityManager.clear();
        statistics.clear();
        String assistantJson = mockMvc.perform(get("/api/operations/support-tickets/assistant-conversation")
                        .header("Authorization", "Bearer pending-support-token"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long assistantStatements = statistics.getPrepareStatementCount();
        JsonNode assistant = objectMapper.readTree(assistantJson);
        UUID conversationId = UUID.fromString(assistant.path("data").path("conversationId").asText());

        entityManager.clear();
        statistics.clear();
        mockMvc.perform(get("/api/operations/chat/conversations/{conversationId}/messages", conversationId)
                        .header("Authorization", "Bearer pending-support-token"))
                .andExpect(status().isOk());
        long historyStatements = statistics.getPrepareStatementCount();
        entityManager.clear();
        statistics.clear();
        mockMvc.perform(post("/api/operations/chat/conversations/{conversationId}/messages", conversationId)
                        .header("Authorization", "Bearer pending-support-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"I need onboarding help\"}"))
                .andExpect(status().isCreated());
        long sendStatements = statistics.getPrepareStatementCount();
        assertEquals(24L, createStatements,
                "authorization (5), identity (1), idempotent ticket write and notification fan-out");
        assertEquals(15L, assistantStatements,
                "authorization (5), identity (1), assistant conversation/member lookup and creation");
        assertEquals(9L, historyStatements,
                "authorization (5), conversation ownership/membership and history read");
        assertEquals(16L, sendStatements,
                "authorization (5), membership recheck, message write and assistant job enqueue");
    }

    @Test
    void rejectedCustomerCanUseSupportButNotAssignedBusinessPermission() throws Exception {
        Set<String> assigned = new java.util.HashSet<>(SUPPORT_PERMISSIONS);
        assigned.add("CUSTOMER_VEHICLE_READ_OWN");
        assigned.add("AI_KNOWLEDGE_READ_CUSTOMER");
        Actor actor = createActor("REJECTED", true, assigned);
        bindToken("rejected-support-token", actor.accountId());

        entityManager.clear();
        statistics.clear();
        mockMvc.perform(get("/api/operations/support-tickets/mine")
                        .header("Authorization", "Bearer rejected-support-token"))
                .andExpect(status().isOk());
        assertEquals(7L, statistics.getPrepareStatementCount());

        String currentAccess = mockMvc.perform(get("/api/iam/accounts/current-access")
                        .header("Authorization", "Bearer rejected-support-token"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode permissions = objectMapper.readTree(currentAccess).path("data").path("permissionCodes");
        assertTrue(contains(permissions, "SUPPORT_TICKET_READ_OWN"));
        assertTrue(!contains(permissions, "CUSTOMER_VEHICLE_READ_OWN"));
        assertTrue(!contains(permissions, "AI_KNOWLEDGE_READ_CUSTOMER"));
    }

    @Test
    void pendingCustomerWithoutSupportPermissionIsDenied() throws Exception {
        Actor actor = createActor("PENDING", true, Set.of("CUSTOMER_VEHICLE_READ_OWN"));
        bindToken("pending-no-support-token", actor.accountId());

        mockMvc.perform(get("/api/operations/support-tickets/mine")
                        .header("Authorization", "Bearer pending-no-support-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void inactiveRoleIsDeniedEvenWhenSupportPermissionIsAssigned() throws Exception {
        Actor actor = createActor("PENDING", false, SUPPORT_PERMISSIONS);
        bindToken("inactive-role-token", actor.accountId());

        mockMvc.perform(get("/api/operations/support-tickets/mine")
                        .header("Authorization", "Bearer inactive-role-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingCustomerIdentityReturnsStableBusinessError() throws Exception {
        Actor actor = createActorWithoutCustomer(SUPPORT_PERMISSIONS);
        bindToken("missing-customer-token", actor.accountId());

        mockMvc.perform(get("/api/operations/support-tickets/mine")
                        .header("Authorization", "Bearer missing-customer-token"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void lockedAndDisabledAccountsAreDeniedEvenWithSupportPermission() throws Exception {
        Actor locked = createActor("PENDING", true, SUPPORT_PERMISSIONS, "LOCKED");
        bindToken("locked-support-token", locked.accountId());
        mockMvc.perform(get("/api/operations/support-tickets/mine")
                        .header("Authorization", "Bearer locked-support-token"))
                .andExpect(status().isForbidden());

        Actor disabled = createActor("PENDING", true, SUPPORT_PERMISSIONS, "DISABLED");
        bindToken("disabled-support-token", disabled.accountId());
        mockMvc.perform(get("/api/operations/support-tickets/mine")
                        .header("Authorization", "Bearer disabled-support-token"))
                .andExpect(status().isForbidden());
    }

    private Actor createActor(String approvalStatus, boolean roleActive, Set<String> permissions) {
        return createActor(approvalStatus, roleActive, permissions, "PENDING");
    }

    private Actor createActor(
            String approvalStatus,
            boolean roleActive,
            Set<String> permissions,
            String accountStatus
    ) {
        UUID roleId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        String roleCode = "TEST_SUPPORT_" + accountId.toString().substring(0, 8).toUpperCase();
        jdbcTemplate.update(
                "insert into iam.roles (role_id, code, name, is_system, is_active, created_at) "
                        + "values (?, ?, ?, false, ?, now())",
                roleId, roleCode, roleCode, roleActive);
        permissions.forEach(permission -> jdbcTemplate.update(
                "insert into iam.role_permissions (id, role_id, permission_id, created_at, is_active, is_system) "
                        + "select ?, ?, permission_id, now(), true, false from iam.permissions where permission_code = ?",
                UUID.randomUUID(), roleId, permission));
        jdbcTemplate.update(
                "insert into people.user_profiles (user_profile_id, full_name, status, created_at) "
                        + "values (?, ?, 'ACTIVE', now())",
                profileId, "Support Flow Customer");
        jdbcTemplate.update(
                "insert into people.customers "
                        + "(customer_id, user_profile_id, customer_code, customer_type, status, approval_status, created_at) "
                        + "values (?, ?, ?, 'REGISTERED', 'INACTIVE', ?, now())",
                customerId, profileId, "CUS-" + accountId, approvalStatus);
        jdbcTemplate.update(
                "insert into iam.accounts "
                        + "(account_id, user_profile_id, username, email, keycloak_user_id, role_id, status, failed_login_count, created_at) "
                        + "values (?, ?, ?, ?, ?, ?, ?, 0, now())",
                accountId, profileId, "support-" + accountId, "support-" + accountId + "@example.com",
                "subject-" + accountId, roleId, accountStatus);
        return new Actor(accountId, customerId);
    }

    private Actor createActorWithoutCustomer(Set<String> permissions) {
        UUID roleId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        String roleCode = "TEST_SUPPORT_NO_CUSTOMER_" + accountId.toString().substring(0, 8).toUpperCase();
        jdbcTemplate.update(
                "insert into iam.roles (role_id, code, name, is_system, is_active, created_at) "
                        + "values (?, ?, ?, false, true, now())",
                roleId, roleCode, roleCode);
        permissions.forEach(permission -> jdbcTemplate.update(
                "insert into iam.role_permissions (id, role_id, permission_id, created_at, is_active, is_system) "
                        + "select ?, ?, permission_id, now(), true, false from iam.permissions where permission_code = ?",
                UUID.randomUUID(), roleId, permission));
        jdbcTemplate.update(
                "insert into people.user_profiles (user_profile_id, full_name, status, created_at) "
                        + "values (?, ?, 'ACTIVE', now())",
                profileId, "Support Flow Without Customer");
        jdbcTemplate.update(
                "insert into iam.accounts "
                        + "(account_id, user_profile_id, username, email, keycloak_user_id, role_id, status, failed_login_count, created_at) "
                        + "values (?, ?, ?, ?, ?, ?, 'ACTIVE', 0, now())",
                accountId, profileId, "support-" + accountId, "support-" + accountId + "@example.com",
                "subject-" + accountId, roleId);
        return new Actor(accountId, null);
    }

    private void bindToken(String token, UUID accountId) {
        when(jwtDecoder.decode(token)).thenReturn(jwt(accountId));
    }

    private Jwt jwt(UUID accountId) {
        Instant issuedAt = Instant.now();
        return new Jwt("token", issuedAt, issuedAt.plusSeconds(300), Map.of("alg", "none"), Map.of(
                "sub", "subject-" + accountId,
                "account_id", accountId.toString(),
                "preferred_username", "support-" + accountId));
    }

    private boolean contains(JsonNode array, String value) {
        for (JsonNode item : array) {
            if (value.equals(item.asText())) return true;
        }
        return false;
    }

    private record Actor(UUID accountId, UUID customerId) {
    }
}
