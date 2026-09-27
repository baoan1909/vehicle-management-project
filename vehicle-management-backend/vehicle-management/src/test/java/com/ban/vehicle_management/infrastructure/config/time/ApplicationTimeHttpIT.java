package com.ban.vehicle_management.infrastructure.config.time;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.time-zone=Asia/Ho_Chi_Minh")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationTimeHttpIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldSerializeApiResponseTimestampUsingApplicationTimezone() throws Exception {
        mockMvc.perform(get("/api/public/application-time"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.timeZone").value("Asia/Ho_Chi_Minh"))
                .andExpect(jsonPath("$.timestamp").value(endsWith("+07:00")));
    }
}
