package com.ban.vehicle_management;

import com.ban.vehicle_management.application.iam.account.port.out.SocialAccountRegistrationPortOut;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatConversationPortOut;
import com.ban.vehicle_management.application.operations.supportticket.service.SupportTicketConversationService;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketPortOut;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.shared.enumeration.iam.SocialIdentityProvider;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationType;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
class VehicleManagementApplicationTests {

	@Autowired
	private SocialAccountRegistrationPortOut socialAccountRegistrationPortOut;

	@Autowired
	private ChatConversationPortOut chatConversationPortOut;

	@Autowired
	private SupportTicketPortOut supportTicketPortOut;

	@Autowired
	private SupportTicketConversationService supportTicketConversationService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private CorsConfigurationSource corsConfigurationSource;

	@Autowired
	private DataSource dataSource;

	@Test
	void contextLoads() {
	}

	@Test
	@Transactional
	void socialAccountRegistrationLockExecutesOnPostgres() {
		socialAccountRegistrationPortOut.lockRegistration(
				SocialIdentityProvider.GOOGLE,
				"integration-test-google-subject",
				"integration-test@example.com"
		);
	}

	@Test
	@Transactional
	void customerSupportLockExecutesOnPostgres() {
		chatConversationPortOut.lockCustomerSupport(UUID.randomUUID());
		supportTicketPortOut.lockCustomerSupport(UUID.randomUUID());
	}

	@Test
	void supportTicketIdempotencyHeaderIsAllowedByCors() {
		MockHttpServletRequest request = new MockHttpServletRequest(
				"OPTIONS",
				"/api/operations/support-tickets/chat-intake"
		);
		var configuration = corsConfigurationSource.getCorsConfiguration(request);

		org.junit.jupiter.api.Assertions.assertNotNull(configuration);
		org.junit.jupiter.api.Assertions.assertTrue(
				configuration.getAllowedHeaders().stream()
						.anyMatch("Idempotency-Key"::equalsIgnoreCase)
		);
	}

	@Test
	@Transactional
	void assistantSupportConversationIsCreatedAndReusedOnPostgres() {
		UUID profileId = UUID.randomUUID();
		UUID accountId = UUID.randomUUID();
		UUID customerId = UUID.randomUUID();
		String uniqueSuffix = accountId.toString();
		UUID customerRoleId = jdbcTemplate.queryForObject(
				"SELECT role_id FROM iam.roles WHERE code = 'CUSTOMER'",
				UUID.class
		);

		jdbcTemplate.update(
				"INSERT INTO people.user_profiles (user_profile_id, full_name) VALUES (?, ?)",
				profileId, "Assistant integration customer"
		);
		jdbcTemplate.update(
				"INSERT INTO iam.accounts (account_id, user_profile_id, username, email, role_id) VALUES (?, ?, ?, ?, ?)",
				accountId, profileId, "assistant-test-" + uniqueSuffix, "assistant-test-" + uniqueSuffix + "@example.com", customerRoleId
		);
		jdbcTemplate.update(
				"INSERT INTO people.customers (customer_id, user_profile_id, customer_code, approval_status) VALUES (?, ?, ?, 'APPROVED')",
				customerId, profileId, "AST-" + uniqueSuffix
		);

		ChatConversation created = supportTicketConversationService.openOrCreateAssistantConversation(customerId, accountId);
		ChatConversation reused = supportTicketConversationService.openOrCreateAssistantConversation(customerId, accountId);

		org.junit.jupiter.api.Assertions.assertEquals(created.getConversationId(), reused.getConversationId());
		org.junit.jupiter.api.Assertions.assertEquals(ChatConversationType.ASSISTANT_SUPPORT, reused.getConversationType());
		org.junit.jupiter.api.Assertions.assertEquals(ChatConversationStatus.ACTIVE, reused.getStatus());
		org.junit.jupiter.api.Assertions.assertEquals("Trợ lý hỗ trợ CoParking", reused.getTitle());
		org.junit.jupiter.api.Assertions.assertEquals(1, reused.getParticipants().size());
	}

	@Test
	void flywayMigrationsApplyToFreshDisposableDatabase() throws SQLException {
		HikariDataSource hikariDataSource = dataSource.unwrap(HikariDataSource.class);
		String databaseName = "vm_flyway_" + UUID.randomUUID().toString().replace("-", "");
		String currentUrl = hikariDataSource.getJdbcUrl();
		String query = currentUrl.contains("?") ? currentUrl.substring(currentUrl.indexOf('?')) : "";
		String baseUrl = query.isEmpty() ? currentUrl : currentUrl.substring(0, currentUrl.indexOf('?'));
		String serverUrl = baseUrl.substring(0, baseUrl.lastIndexOf('/') + 1);
		String adminUrl = serverUrl + "postgres" + query;
		String freshUrl = serverUrl + databaseName + query;
		boolean created = false;

		try (Connection admin = DriverManager.getConnection(
				adminUrl, hikariDataSource.getUsername(), hikariDataSource.getPassword());
			 Statement statement = admin.createStatement()) {
			statement.execute("CREATE DATABASE " + databaseName);
			created = true;
		}

		try {
			Flyway flyway = Flyway.configure()
					.dataSource(freshUrl, hikariDataSource.getUsername(), hikariDataSource.getPassword())
					.locations("classpath:db/migration")
					.defaultSchema("public")
					.schemas("public")
					.validateMigrationNaming(true)
					.cleanDisabled(true)
					.load();
			flyway.migrate();
			org.junit.jupiter.api.Assertions.assertEquals(
					"20260906110000",
					flyway.info().current().getVersion().toString()
			);
			UUID ticketId = UUID.randomUUID();
			try (Connection fresh = DriverManager.getConnection(
					freshUrl, hikariDataSource.getUsername(), hikariDataSource.getPassword());
				 Statement statement = fresh.createStatement()) {
				statement.executeUpdate("""
						INSERT INTO operations.approval_requests
						(approval_request_id, request_type, target_schema, target_table, target_id, status, request_data)
						VALUES ('%s', 'SUPPORT_TICKET_ESCALATION', 'operations', 'support_tickets', '%s', 'PENDING', '{}'::jsonb)
						""".formatted(UUID.randomUUID(), ticketId));
				org.junit.jupiter.api.Assertions.assertThrows(SQLException.class, () -> statement.executeUpdate("""
						INSERT INTO operations.approval_requests
						(approval_request_id, request_type, target_schema, target_table, target_id, status, request_data)
						VALUES ('%s', 'SUPPORT_TICKET_ESCALATION', 'operations', 'support_tickets', '%s', 'PENDING', '{}'::jsonb)
						""".formatted(UUID.randomUUID(), ticketId)));
			}
		} finally {
			if (created) {
				try (Connection admin = DriverManager.getConnection(
						adminUrl, hikariDataSource.getUsername(), hikariDataSource.getPassword());
					 Statement statement = admin.createStatement()) {
					statement.execute("DROP DATABASE " + databaseName + " WITH (FORCE)");
				}
			}
		}
	}

}


