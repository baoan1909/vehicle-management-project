package com.ban.vehicle_management.entrypoint.operations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ban.vehicle_management.application.operations.supportticketcategory.port.in.SupportTicketCategoryPortIn;
import com.ban.vehicle_management.application.operations.supportticketcategory.port.out.SupportTicketCategoryPortOut;
import com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {"spring.jpa.properties.hibernate.generate_statistics=true", "spring.jpa.show-sql=false"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SupportCategoryCatalogIT extends PostgresIntegrationTestSupport {
    private static final String BASE = "/api/operations/support-ticket-categories";
    private static final String INTERNAL = "SUPPORT_TICKET_CATEGORY_INTERNAL_OWN";
    private static final String CUSTOMER = "SUPPORT_TICKET_CATEGORY_CUSTOMER_OWN";
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired SupportTicketCategoryPortOut categories;
    @Autowired SupportTicketCategoryPortIn useCase;
    @Autowired EntityManager entityManager;
    @Autowired EntityManagerFactory emf;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean TaskScheduler scheduler;

    @Test
    void migrationCreatesExactCatalogAndLostCardPermissions() {
        assertEquals(10, count("select count(*) from operations.support_ticket_categories where code like 'CUSTOMER_%' and status='ACTIVE'"));
        assertEquals(13, count("select count(*) from operations.support_ticket_categories where code like 'INTERNAL_%' and status='ACTIVE'"));
        assertEquals(List.of("LOST_CARD_REPORT_CREATE_ALL", "LOST_CARD_REPORT_READ_ALL", "LOST_CARD_REPORT_UPDATE_ALL"),
                jdbc.queryForList("""
                        select p.permission_code from operations.support_ticket_category_permissions m
                        join iam.permissions p on p.permission_id=m.permission_id
                        join operations.support_ticket_categories c on c.category_id=m.category_id
                        where c.code='INTERNAL_LOST_CARD_WORKFLOW' order by p.permission_code
                        """, String.class));
        assertEquals("INACTIVE", jdbc.queryForObject("select status from operations.support_ticket_categories where code='LOST_CARD'", String.class));
        assertNotEquals(id("LOST_CARD"), id("INTERNAL_LOST_CARD_WORKFLOW"));
    }

    @Test
    void customerAndGuestOnlySeeCustomerCategoriesAndCannotForgeInternalId() throws Exception {
        String token = actor(CUSTOMER);
        JsonNode available = available(token);
        assertEquals(10, canonicalCustomerCount(available));
        available.forEach(category -> assertFalse(category.path("code").asText().startsWith("INTERNAL_")));
        JsonNode publicCategories = json.readTree(mvc.perform(get("/api/public/support/categories"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data");
        assertEquals(10, canonicalCustomerCount(publicCategories));
        publicCategories.forEach(category -> assertFalse(category.path("code").asText().startsWith("INTERNAL_")));
        createTicket(token, id("INTERNAL_LOST_CARD_WORKFLOW"), 403);
        createTicket(token, id("INTERNAL_OTHER_SYSTEM"), 403);
        assertThrows(com.ban.vehicle_management.shared.exception.BadRequestException.class,
                () -> useCase.ensurePublicAvailableCategory(id("INTERNAL_LOST_CARD_WORKFLOW")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"LOST_CARD_REPORT_READ_ALL", "LOST_CARD_REPORT_CREATE_ALL", "LOST_CARD_REPORT_UPDATE_ALL"})
    void internalLostCardAnyPermissionAllowsRealTicketCreation(String permission) throws Exception {
        String token = actor(INTERNAL, permission);
        JsonNode available = available(token);
        assertTrue(contains(available, "INTERNAL_LOST_CARD_WORKFLOW"));
        assertFalse(contains(available, "CUSTOMER_OTHER_SYSTEM"));
        createTicket(token, id("INTERNAL_LOST_CARD_WORKFLOW"), 201);
        createTicket(token, id("CUSTOMER_OTHER_SYSTEM"), 403);
    }

    @Test
    void missingFeaturePermissionAndInactiveCategoryAreRejectedAtCreation() throws Exception {
        String token = actor(INTERNAL);
        assertFalse(contains(available(token), "INTERNAL_LOST_CARD_WORKFLOW"));
        createTicket(token, id("INTERNAL_LOST_CARD_WORKFLOW"), 403);
        jdbc.update("update operations.support_ticket_categories set status='INACTIVE' where code='INTERNAL_OTHER_SYSTEM'");
        entityManager.clear();
        createTicket(token, id("INTERNAL_OTHER_SYSTEM"), 409);
    }

    @Test
    void adminCrudCustomCategoryIsImmediatelyAvailableAndSoftDeletePreservesHistory() throws Exception {
        String admin = actor("SUPPORT_TICKET_CATEGORY_READ_ALL", "SUPPORT_TICKET_CATEGORY_CREATE_ALL",
                "SUPPORT_TICKET_CATEGORY_UPDATE_ALL", "SUPPORT_TICKET_CATEGORY_ACTIVATE_ALL");
        Map<String, Object> body = new HashMap<>(Map.of("code", "CUSTOM_NEW_SYSTEM_ERROR", "name", "Lỗi chức năng mới",
                "audienceType", "CUSTOMER", "priority", "HIGH", "permissionMatchMode", "ALL", "displayOrder", 15,
                "requiredPermissionCodes", List.of()));
        JsonNode created = json.readTree(mvc.perform(post(BASE).header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("data");
        UUID categoryId = UUID.fromString(created.path("categoryId").asText());
        mvc.perform(get(BASE + "/" + categoryId).header("Authorization", "Bearer " + admin)).andExpect(status().isOk());
        String customer = actor(CUSTOMER);
        assertTrue(contains(available(customer), "CUSTOM_NEW_SYSTEM_ERROR"));
        body.put("displayOrder", 5);
        mvc.perform(put(BASE + "/" + categoryId).header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andExpect(status().isOk());
        JsonNode sorted = available(customer);
        assertTrue(indexOf(sorted, "CUSTOM_NEW_SYSTEM_ERROR") < indexOf(sorted, "CUSTOMER_ACCOUNT_ACCESS"));
        createTicket(customer, categoryId, 201);
        mvc.perform(delete(BASE + "/" + categoryId).header("Authorization", "Bearer " + admin)).andExpect(status().isOk());
        assertFalse(contains(available(customer), "CUSTOM_NEW_SYSTEM_ERROR"));
        JsonNode tickets = json.readTree(mvc.perform(get("/api/operations/support-tickets/mine")
                        .header("Authorization", "Bearer " + customer)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data");
        assertEquals("Lỗi chức năng mới", tickets.get(0).path("categoryName").asText());
        createTicket(customer, categoryId, 409);
        mvc.perform(patch(BASE + "/" + categoryId + "/activate").header("Authorization", "Bearer " + admin)).andExpect(status().isOk());
        assertTrue(contains(available(customer), "CUSTOM_NEW_SYSTEM_ERROR"));
        mvc.perform(post(BASE).header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))).andExpect(status().isConflict());
        mvc.perform(post(BASE).header("Authorization", "Bearer " + customer).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))).andExpect(status().isForbidden());
    }

    @Test
    void categoryPermissionListUsesTwoQueriesRegardlessOfCatalogSize() {
        entityManager.clear();
        var statistics = emf.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        var list = categories.findAll(null, null, null);
        assertTrue(list.size() >= 41);
        assertEquals(2, statistics.getPrepareStatementCount(), "one category query plus one bulk permission query");
        assertEquals(3, list.stream().filter(c -> c.getCode().equals("INTERNAL_LOST_CARD_WORKFLOW"))
                .findFirst().orElseThrow().getRequiredPermissionCodes().size());
    }

    @Test
    void migrationReapplicationPreservesHistoricalIdsCustomCategoryAndCustomizedSeed() throws Exception {
        UUID legacyId = id("LOST_CARD");
        String requester = actor(CUSTOMER);
        UUID accountId = jdbc.queryForObject("select account_id from iam.accounts where keycloak_user_id=?", UUID.class, requester);
        UUID historicalId = UUID.randomUUID();
        jdbc.update("""
                insert into operations.support_tickets(support_ticket_id, category_id, requester_account_id,
                requester_role_code, title, content, status, source) values (?, ?, ?, 'CUSTOM_ROLE', 'Historical', 'Lost card', 'OPEN', 'SELF_SERVICE_PORTAL')
                """, historicalId, legacyId, accountId);
        jdbc.update("update operations.support_ticket_categories set status='ACTIVE' where code='LOST_CARD'");
        jdbc.update("update operations.support_ticket_categories set status='ACTIVE', name='Danh mục đã tùy chỉnh' where code='OTHER'");
        jdbc.update("insert into operations.support_ticket_categories(code,name,priority,status,audience_type) values ('CUSTOM_LEGACY','Custom','NORMAL','ACTIVE','CUSTOMER')");
        String migration = new ClassPathResource("db/migration/V20261119100000__standardize_support_category_catalog.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        jdbc.execute(migration);
        assertEquals(legacyId, jdbc.queryForObject("select category_id from operations.support_tickets where support_ticket_id=?", UUID.class, historicalId));
        assertEquals("Mất thẻ xe", jdbc.queryForObject("select name from operations.support_ticket_categories where category_id=?", String.class, legacyId));
        assertEquals("INACTIVE", jdbc.queryForObject("select status from operations.support_ticket_categories where category_id=?", String.class, legacyId));
        assertEquals("ACTIVE", jdbc.queryForObject("select status from operations.support_ticket_categories where code='CUSTOM_LEGACY'", String.class));
        assertEquals("ACTIVE", jdbc.queryForObject("select status from operations.support_ticket_categories where code='OTHER'", String.class));
        assertEquals(23, count("select count(*) from operations.support_ticket_categories where code like 'CUSTOMER_%' or code like 'INTERNAL_%'"));
        assertEquals(30, count("select count(*) from operations.support_ticket_category_permissions m join operations.support_ticket_categories c on c.category_id=m.category_id where c.code like 'INTERNAL_%'"));
    }

    private int count(String sql) { return Objects.requireNonNull(jdbc.queryForObject(sql, Integer.class)); }

    @Test
    void realEffectivePermissionsApplyAllThenAnyWithoutFrontendChanges() throws Exception {
        UUID categoryId = UUID.randomUUID();
        jdbc.update("""
                insert into operations.support_ticket_categories(category_id,code,name,priority,status,audience_type,permission_match_mode)
                values (?,'INTERNAL_CUSTOM_ALL','Custom all','NORMAL','ACTIVE','INTERNAL','ALL')
                """, categoryId);
        jdbc.update("""
                insert into operations.support_ticket_category_permissions(category_id,permission_id)
                select ?,permission_id from iam.permissions
                where permission_code in ('LOST_CARD_REPORT_READ_ALL','LOST_CARD_REPORT_CREATE_ALL')
                """, categoryId);
        String partial = actor(INTERNAL, "LOST_CARD_REPORT_READ_ALL");
        assertFalse(contains(available(partial), "INTERNAL_CUSTOM_ALL"));
        createTicket(partial, categoryId, 403);
        String full = actor(INTERNAL, "LOST_CARD_REPORT_READ_ALL", "LOST_CARD_REPORT_CREATE_ALL");
        assertTrue(contains(available(full), "INTERNAL_CUSTOM_ALL"));
        jdbc.update("update operations.support_ticket_categories set permission_match_mode='ANY' where category_id=?", categoryId);
        entityManager.clear();
        assertTrue(contains(available(partial), "INTERNAL_CUSTOM_ALL"));
    }

    @Test
    void reservedCodeCollisionFailsInsteadOfOverwritingAdminCategory() throws Exception {
        jdbc.update("delete from operations.support_ticket_categories where code='CUSTOMER_ACCOUNT_ACCESS'");
        jdbc.update("""
                insert into operations.support_ticket_categories(code,name,priority,status,audience_type)
                values ('CUSTOMER_ACCOUNT_ACCESS','Custom reserved code','LOW','ACTIVE','CUSTOMER')
                """);
        String migration = new ClassPathResource("db/migration/V20261119100000__standardize_support_category_catalog.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        var error = assertThrows(org.springframework.dao.DataAccessException.class, () -> jdbc.execute(migration));
        assertTrue(error.getMostSpecificCause().getMessage().contains("code collision"));
    }

    private UUID id(String code) { return jdbc.queryForObject("select category_id from operations.support_ticket_categories where code=?", UUID.class, code); }
    private boolean contains(JsonNode values, String code) {
        for (JsonNode value : values) if (value.path("code").asText().equals(code)) return true;
        return false;
    }
    private int canonicalCustomerCount(JsonNode values) {
        int count = 0;
        for (JsonNode value : values) if (value.path("code").asText().startsWith("CUSTOMER_")) count++;
        return count;
    }
    private int indexOf(JsonNode values, String code) {
        for (int i = 0; i < values.size(); i++) if (values.get(i).path("code").asText().equals(code)) return i;
        return -1;
    }
    private JsonNode available(String token) throws Exception {
        return json.readTree(mvc.perform(get(BASE + "/available").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data");
    }
    private void createTicket(String token, UUID categoryId, int expectedStatus) throws Exception {
        mvc.perform(post("/api/operations/support-tickets").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "categoryId", categoryId, "title", "Lỗi chức năng", "content", "Không thể hoàn tất chức năng đang sử dụng."))))
                .andExpect(status().is(expectedStatus));
    }
    private String actor(String... permissions) {
        UUID roleId = UUID.randomUUID(), profileId = UUID.randomUUID(), accountId = UUID.randomUUID();
        String token = "category-" + accountId;
        jdbc.update("insert into iam.roles(role_id,code,name,is_active,is_system) values (?,?,?,true,false)",
                roleId, "CATALOG_TEST_" + accountId.toString().substring(0, 8).toUpperCase(Locale.ROOT), token);
        Set<String> effective = new HashSet<>(List.of(permissions));
        effective.add("SUPPORT_TICKET_CREATE_OWN");
        effective.add("SUPPORT_TICKET_READ_OWN");
        for (String code : effective) {
            assertEquals(1, jdbc.update("insert into iam.role_permissions(id,role_id,permission_id,is_active,is_system) select ?,?,permission_id,true,false from iam.permissions where permission_code=?", UUID.randomUUID(), roleId, code));
        }
        jdbc.update("insert into people.user_profiles(user_profile_id,full_name,status) values (?,?,'ACTIVE')", profileId, token);
        jdbc.update("insert into iam.accounts(account_id,user_profile_id,username,email,keycloak_user_id,role_id,status) values (?,?,?,?,?,?,'ACTIVE')",
                accountId, profileId, token, token + "@example.com", token, roleId);
        when(jwtDecoder.decode(token)).thenReturn(new Jwt(token, Instant.now(), Instant.now().plusSeconds(600),
                Map.of("alg", "none"), Map.of("sub", token, "account_id", accountId.toString(), "preferred_username", token)));
        return token;
    }
}
