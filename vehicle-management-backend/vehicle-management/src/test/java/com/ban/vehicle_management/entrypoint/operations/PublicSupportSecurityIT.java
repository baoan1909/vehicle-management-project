package com.ban.vehicle_management.entrypoint.operations;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicSupportSecurityIT extends PostgresIntegrationTestSupport {
    private static final String SYSTEM_ADMIN_SUBJECT = "54fd8e18-9f9d-4d93-8f63-f4f0374da001";
    @Autowired MockMvc mockMvc;

    @Test
    void guestCanReadOnlyActivePublicCategoriesAndAssistantStatus() throws Exception {
        mockMvc.perform(get("/api/public/support/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        mockMvc.perform(get("/api/public/support/assistant/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").isBoolean());
    }

    @Test
    void invalidGuestIntakeIsValidatedWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/public/support/requests")
                        .header("Idempotency-Key", "security-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void categoryMutationsRejectGuestAndAuthenticatedCallerWithoutPermission() throws Exception {
        UUID categoryId = UUID.randomUUID();
        mockMvc.perform(delete("/api/operations/support-ticket-categories/" + categoryId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/operations/support-ticket-categories/" + categoryId).with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void accountWithEffectiveCategoryManagementPermissionCanCreateCategory() throws Exception {
        String uniqueCode = "GUEST_SECURITY_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        mockMvc.perform(post("/api/operations/support-ticket-categories")
                        .with(jwt().jwt(builder -> builder.subject(SYSTEM_ADMIN_SUBJECT)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "%s",
                                  "name": "Guest security test",
                                  "description": "Permission-positive integration test",
                                  "priority": "NORMAL"
                                }
                                """.formatted(uniqueCode)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value(uniqueCode.toUpperCase(java.util.Locale.ROOT)));
    }
}
