package com.ban.vehicle_management.entrypoint.parking;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(properties = {
        "app.maps.rate-limit.enabled=true",
        "app.maps.rate-limit.anonymous-ip-limit=2",
        "app.maps.rate-limit.authenticated-ip-limit=2",
        "app.maps.rate-limit.account-limit=2",
        "app.redis.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ParkingLocationApiSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposePublicRolloutStatusWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/public/parking-map/features"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerParkingMapEnabled").value(true))
                .andExpect(jsonPath("$.data.publicNearbySearchEnabled").value(true))
                .andExpect(jsonPath("$.data.geocodingProviderEnabled").value(true));
    }

    @Test
    void shouldRejectUnauthenticatedAndUnauthorizedAdminGeocoding() throws Exception {
        String path = "/api/parking/parking-lots/" + UUID.randomUUID() + "/geocode";

        mockMvc.perform(post(path))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(path).with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectNumericSqlInjectionPayloadAsBadRequest() throws Exception {
        mockMvc.perform(get("/api/public/parking-lots/nearby")
                        .with(remoteAddress("203.0.113.21"))
                        .queryParam("latitude", "10.85 OR 1=1")
                        .queryParam("longitude", "106.77"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnStructured429AfterAnonymousLimit() throws Exception {
        RequestPostProcessor remoteAddress = remoteAddress("203.0.113.22");

        for (int request = 0; request < 2; request++) {
            mockMvc.perform(get("/api/public/parking-lots/nearby")
                            .with(remoteAddress)
                            .queryParam("latitude", "10.776889")
                            .queryParam("longitude", "106.700806"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/public/parking-lots/nearby")
                        .with(remoteAddress)
                        .queryParam("latitude", "10.776889")
                        .queryParam("longitude", "106.700806"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.data.code").value("MAP_RATE_LIMIT_EXCEEDED"));
    }

    private RequestPostProcessor remoteAddress(String value) {
        return request -> {
            request.setRemoteAddr(value);
            return request;
        };
    }
}