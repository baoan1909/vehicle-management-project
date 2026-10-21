package com.ban.vehicle_management.entrypoint.notification;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class UserGuideAdminSecurityIT extends com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRejectUnauthenticatedUserGuideAdminAccess() throws Exception {
        mockMvc.perform(get("/api/notifications/user-guides"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/notifications/user-guides/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/notifications/user-guides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/notifications/user-guide-features"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/notifications/user-guide-features")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectAuthenticatedWithoutUserGuidePermission() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(get("/api/notifications/user-guides").with(jwt()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/notifications/user-guides/" + randomId).with(jwt()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/notifications/user-guides").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"featureId\":\"" + randomId + "\","
                                + "\"code\":\"GUIDE_ONE\","
                                + "\"contextKey\":\"PARKING_CHECKIN\","
                                + "\"displayType\":\"MODAL\","
                                + "\"replayPolicy\":\"ONCE_PER_VERSION\","
                                + "\"startAt\":\"2026-01-01T00:00:00Z\","
                                + "\"initialVersion\":{\"title\":\"Version one\","
                                + "\"audienceMode\":\"ALL_AUTHENTICATED\","
                                + "\"steps\":[{\"displayOrder\":0,\"title\":\"Step one\",\"body\":\"Do this\",\"placement\":\"CENTER\"}]}}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/notifications/user-guides/" + randomId).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lockVersion\":1}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/notifications/user-guides/" + randomId + "/versions/1/publish").with(jwt()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/notifications/user-guides/" + randomId + "/archive").with(jwt()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/notifications/user-guide-features").with(jwt()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/notifications/user-guide-features").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"X\",\"name\":\"X\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectUnauthenticatedRuntimeAccess() throws Exception {
        mockMvc.perform(get("/api/user-guides/resolve"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/user-guides/SOME_CODE"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/user-guides/" + UUID.randomUUID() + "/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"STARTED\"}"))
                .andExpect(status().isUnauthorized());
    }
}


