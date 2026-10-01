package com.ban.vehicle_management.reference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdministrativeDivisionIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldImportBothDatasetsWithExpectedCountsVersionsAndIntegrity() {
        assertEquals(5, count("reference.administrative_units"));
        assertEquals(34, count("reference.provinces"));
        assertEquals(3321, count("reference.wards"));
        assertEquals(63, count("reference.legacy_provinces"));
        assertEquals(696, count("reference.legacy_districts"));
        assertEquals(10035, count("reference.legacy_wards"));

        assertEquals("v5.2.0", jdbcTemplate.queryForObject("""
                SELECT dataset_version FROM reference.administrative_dataset_metadata WHERE dataset_key = 'CURRENT'
                """, String.class));
        assertEquals("v2.4.1", jdbcTemplate.queryForObject("""
                SELECT dataset_version FROM reference.legacy_dataset_metadata WHERE dataset_key = 'LEGACY'
                """, String.class));

        assertEquals(0, scalar("""
                SELECT count(*) FROM reference.wards w
                LEFT JOIN reference.provinces p ON p.code = w.province_code
                WHERE p.code IS NULL
                """));
        assertEquals(0, scalar("""
                SELECT count(*) FROM reference.legacy_districts d
                LEFT JOIN reference.legacy_provinces p ON p.code = d.province_code
                WHERE p.code IS NULL
                """));
        assertEquals(0, scalar("""
                SELECT count(*) FROM reference.legacy_wards w
                LEFT JOIN reference.legacy_districts d ON d.code = w.district_code
                WHERE d.code IS NULL
                """));
        assertEquals(0, scalar("""
                SELECT count(*) FROM (
                    SELECT name, full_name FROM reference.provinces
                    UNION ALL SELECT name, full_name FROM reference.wards
                    UNION ALL SELECT name, full_name FROM reference.legacy_provinces
                    UNION ALL SELECT name, full_name FROM reference.legacy_districts
                    UNION ALL SELECT name, full_name FROM reference.legacy_wards
                ) names WHERE btrim(name) = '' OR btrim(full_name) = ''
                """));

        assertTrue(exists("SELECT EXISTS (SELECT 1 FROM reference.provinces WHERE code = '01')"));
        assertTrue(exists("SELECT EXISTS (SELECT 1 FROM reference.legacy_districts WHERE code = '001')"));
        assertTrue(exists("SELECT EXISTS (SELECT 1 FROM reference.legacy_wards WHERE code = '00001')"));
    }

    @Test
    void shouldExposeExactlyTheFivePublicHierarchyEndpointsWithoutAuthentication() throws Exception {
        assertEndpointMatchesQuery(
                "/api/public/administrative-divisions/current/provinces",
                "SELECT code FROM reference.provinces"
        );
        assertEndpointMatchesQuery(
                "/api/public/administrative-divisions/current/provinces/01/wards",
                "SELECT code FROM reference.wards WHERE province_code = '01'"
        );
        assertEndpointMatchesQuery(
                "/api/public/administrative-divisions/legacy/provinces",
                "SELECT code FROM reference.legacy_provinces"
        );
        assertEndpointMatchesQuery(
                "/api/public/administrative-divisions/legacy/provinces/01/districts",
                "SELECT code FROM reference.legacy_districts WHERE province_code = '01'"
        );
        assertEndpointMatchesQuery(
                "/api/public/administrative-divisions/legacy/districts/001/wards",
                "SELECT code FROM reference.legacy_wards WHERE district_code = '001'"
        );
    }

    @Test
    void shouldRejectInvalidCodesAndReturnNotFoundForUnknownParents() throws Exception {
        mockMvc.perform(get("/api/public/administrative-divisions/current/provinces/1/wards"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/public/administrative-divisions/legacy/districts/01/wards"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/public/administrative-divisions/current/provinces/00/wards"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
        mockMvc.perform(get("/api/public/administrative-divisions/legacy/provinces/00/districts"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/public/administrative-divisions/legacy/districts/000/wards"))
                .andExpect(status().isNotFound());
    }

    private void assertEndpointMatchesQuery(String endpoint, String sql) throws Exception {
        Set<String> expectedCodes = new HashSet<>(jdbcTemplate.queryForList(sql, String.class));
        MvcResult result = mockMvc.perform(get(endpoint))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(expectedCodes.size()))
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsByteArray()).path("data");
        Set<String> actualCodes = new HashSet<>();
        data.forEach(item -> {
            actualCodes.add(item.path("code").asText());
            assertTrue(item.hasNonNull("name"));
            assertTrue(item.hasNonNull("fullName"));
            assertEquals(3, item.size(), "Public response must not leak hierarchy or internal metadata");
        });
        assertEquals(expectedCodes, actualCodes);
    }

    private int count(String table) {
        return scalar("SELECT count(*) FROM " + table);
    }

    private int scalar(String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    private boolean exists(String sql) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class));
    }
}
