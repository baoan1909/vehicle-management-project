package com.ban.vehicle_management.application.operations.guestsupport.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.application.operations.supportticketcategory.port.in.SupportTicketCategoryPortIn;
import com.ban.vehicle_management.entrypoint.dto.operations.guestsupport.request.CreateGuestSupportRequest;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryStatus;
import com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport;
import jakarta.persistence.EntityManagerFactory;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "app.public-support.capability-secret=query-count-test-secret-with-32-bytes"
})
@ActiveProfiles("test")
class GuestSupportSqlStatementCountIT extends PostgresIntegrationTestSupport {
    @Autowired GuestSupportService service;
    @Autowired SupportTicketCategoryPortIn categoryPortIn;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired JdbcTemplate jdbcTemplate;
    private Statistics statistics;

    @BeforeEach
    void setUp() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("delete from operations.guest_support_requests where email like 'query-count-%@example.com'");
    }

    @Test
    void createAndCapabilityReadStayWithinQueryBudgets() {
        UUID categoryId = categoryPortIn.getCategories(SupportTicketCategoryStatus.ACTIVE, null, null)
                .getFirst().getCategoryId();
        String suffix = UUID.randomUUID().toString();
        CreateGuestSupportRequest input = new CreateGuestSupportRequest(
                categoryId, "Query count", "Guest support query count", "query-count-" + suffix + "@example.com", null, suffix);

        statistics.clear();
        GuestSupportService.CreatedRequest created = service.create(input, suffix, "127.0.0.1");
        long createStatements = statistics.getPrepareStatementCount();

        statistics.clear();
        service.access(created.request().getPublicReferenceCode(), created.capabilityToken());
        long accessStatements = statistics.getPrepareStatementCount();

        assertTrue(createStatements <= 10, "Guest intake query budget exceeded: " + createStatements);
        assertTrue(accessStatements <= 3, "Guest capability-read query budget exceeded: " + accessStatements);
    }
}
