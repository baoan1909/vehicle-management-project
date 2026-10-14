package com.ban.vehicle_management.entrypoint.billing;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class PlatformFeeAdminSecurityIT extends com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRejectUnauthenticatedPlatformFeeAccess() throws Exception {
        mockMvc.perform(get("/api/billing/admin/platform-fees"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/billing/admin/platform-fees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/billing/admin/platform-fees/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectAuthenticatedWithoutPermission() throws Exception {
        mockMvc.perform(get("/api/billing/admin/platform-fees").with(jwt()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/billing/admin/platform-fees")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feeCode\":\"X\",\"name\":\"X\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/billing/admin/platform-fees/" + UUID.randomUUID()).with(jwt()))
                .andExpect(status().isForbidden());
    }
}
