package com.ban.vehicle_management;

import com.ban.vehicle_management.application.iam.account.port.out.SocialAccountRegistrationPortOut;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatConversationPortOut;
import com.ban.vehicle_management.application.operations.supportticket.service.SupportTicketConversationService;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketPortOut;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.ai.model.AiMessageCitation;
import com.ban.vehicle_management.domain.ai.model.HybridSearchRow;
import com.ban.vehicle_management.domain.ai.model.RetrievalAudit;
import com.ban.vehicle_management.infrastructure.persistence.adapter.ai.AiMessageCitationPersistenceAdapter;
import com.ban.vehicle_management.infrastructure.persistence.adapter.ai.RetrievalAuditPersistenceAdapter;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeChunkRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeDocumentRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeIngestionJobRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeQualityMetricsRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeSourceRepository;
import com.ban.vehicle_management.infrastructure.persistence.specification.ai.KnowledgeDocumentSpecifications;
import com.ban.vehicle_management.infrastructure.persistence.specification.ai.KnowledgeIngestionJobSpecifications;
import com.ban.vehicle_management.infrastructure.persistence.specification.ai.KnowledgeSourceSpecifications;
import com.ban.vehicle_management.shared.enumeration.iam.SocialIdentityProvider;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationType;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.PageRequest;
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

	@Autowired
	private KnowledgeSourceRepository knowledgeSourceRepository;

	@Autowired
	private KnowledgeDocumentRepository knowledgeDocumentRepository;

	@Autowired
	private KnowledgeIngestionJobRepository knowledgeIngestionJobRepository;

	@Autowired
	private KnowledgeQualityMetricsRepository knowledgeQualityMetricsRepository;

	@Autowired
	private KnowledgeChunkRepository knowledgeChunkRepository;

	@Autowired
	private RetrievalAuditPersistenceAdapter retrievalAuditPersistenceAdapter;

	@Autowired
	private AiMessageCitationPersistenceAdapter aiMessageCitationPersistenceAdapter;

	@Test
	void contextLoads() {
	}

	@Test
	@Transactional(readOnly = true)
	void knowledgeFiltersAndDashboardAggregatesExecuteOnPostgres() {
		var page = PageRequest.of(0, 10);
		org.junit.jupiter.api.Assertions.assertNotNull(knowledgeSourceRepository.findAll(
				KnowledgeSourceSpecifications.keyword("hỗ trợ"), page));
		org.junit.jupiter.api.Assertions.assertNotNull(knowledgeDocumentRepository.findAll(
				KnowledgeDocumentSpecifications.keyword("hỗ trợ"), page));
		org.junit.jupiter.api.Assertions.assertNotNull(knowledgeIngestionJobRepository.findAll(
				KnowledgeIngestionJobSpecifications.keyword("hỗ trợ"), page));
		org.junit.jupiter.api.Assertions.assertNotNull(knowledgeQualityMetricsRepository.countSourcesByStatus());
		org.junit.jupiter.api.Assertions.assertNotNull(knowledgeQualityMetricsRepository.countDocumentsByStatus());
		org.junit.jupiter.api.Assertions.assertNotNull(knowledgeQualityMetricsRepository.countJobsByStatus());
		org.junit.jupiter.api.Assertions.assertNotNull(knowledgeQualityMetricsRepository.countIndexVersionsByStatus());
		var now = Instant.now();
		org.junit.jupiter.api.Assertions.assertTrue(knowledgeQualityMetricsRepository.countStuckJobs(now) >= 0);
		org.junit.jupiter.api.Assertions.assertTrue(knowledgeQualityMetricsRepository.countStaleCandidates(now) >= 0);
		org.junit.jupiter.api.Assertions.assertTrue(knowledgeQualityMetricsRepository.countExpiredDocuments(now) >= 0);
		org.junit.jupiter.api.Assertions.assertTrue(knowledgeQualityMetricsRepository.countMissingEmbeddings() >= 0);
		org.junit.jupiter.api.Assertions.assertTrue(knowledgeQualityMetricsRepository.countIncompleteActiveIndexes() >= 0);
		org.junit.jupiter.api.Assertions.assertNotNull(knowledgeQualityMetricsRepository.findActiveModelConfigurationIds());
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
	void immutableUnaccentNormalizesVietnamese() {
		String normalized = jdbcTemplate.queryForObject(
				"SELECT ai.immutable_unaccent('Đăng Ký Thẻ Xe Máy')", String.class);
		org.junit.jupiter.api.Assertions.assertEquals("Dang Ky The Xe May", normalized);
	}

	@Test
	@Transactional
	void hybridSearchRespectsTenantScopeEffectiveDateAndLatestVersion() {
		UUID sourceId = UUID.randomUUID();
		jdbcTemplate.update(
				"INSERT INTO ai.knowledge_sources (source_id, title, access_scope, status) VALUES (?, ?, 'PUBLIC', 'ACTIVE')",
				sourceId, "Hybrid IT source");
		UUID key = UUID.randomUUID();
		UUID docV1 = UUID.randomUUID();
		UUID docV2 = UUID.randomUUID();
		insertReadyDocument(sourceId, key, docV1, 1, null);
		insertReadyDocument(sourceId, key, docV2, 2, null);
		UUID chunkV1 = insertReadyChunk(docV1, 1, null, "quy trình đăng ký thẻ xe máy tại quầy lễ tân");
		UUID chunkV2 = insertReadyChunk(docV2, 2, null, "quy trình đăng ký thẻ xe máy tại quầy lễ tân");
		UUID targetTenant = UUID.randomUUID();
		UUID otherTenant = UUID.randomUUID();
		UUID tenantDoc = UUID.randomUUID();
		insertReadyDocument(sourceId, UUID.randomUUID(), tenantDoc, 1, null);
		UUID tenantChunk = insertReadyChunk(
				tenantDoc, 1, targetTenant, "quy trình đăng ký thẻ xe máy riêng cho đơn vị");
		jdbcTemplate.update("UPDATE ai.knowledge_chunks SET access_scope = 'TENANT_PRIVATE' WHERE chunk_id = ?", tenantChunk);
		UUID otherTenantDoc = UUID.randomUUID();
		insertReadyDocument(sourceId, UUID.randomUUID(), otherTenantDoc, 1, null);
		UUID otherTenantChunk = insertReadyChunk(
				otherTenantDoc, 1, otherTenant, "quy trình đăng ký thẻ xe máy riêng cho đơn vị");
		jdbcTemplate.update("UPDATE ai.knowledge_chunks SET access_scope = 'TENANT_PRIVATE' WHERE chunk_id = ?", otherTenantChunk);
		UUID expiredDoc = UUID.randomUUID();
		jdbcTemplate.update(
				"INSERT INTO ai.knowledge_documents (document_id, source_id, document_key, title, access_scope, status, document_version, effective_to) VALUES (?, ?, ?, ?, 'PUBLIC', 'READY', 1, now() - INTERVAL '1 hour')",
				expiredDoc, sourceId, UUID.randomUUID(), "Expired");
		insertReadyChunk(expiredDoc, 1, null, "quy trình đăng ký thẻ xe máy tại quầy lễ tân");

		UUID modelConfigId = jdbcTemplate.queryForObject(
				"SELECT configuration_id FROM ai.ai_model_configurations WHERE use_case = 'EMBEDDING' AND status = 'ACTIVE' LIMIT 1",
				UUID.class);
		UUID indexVersionId = UUID.randomUUID();
		jdbcTemplate.update(
				"INSERT INTO ai.knowledge_index_versions (index_version_id, version_code, model_configuration_id, provider, model_id, dimension, chunker_version, embedding_prompt_version, distance_metric, normalization, status) VALUES (?, ?, ?, 'GEMINI', 'gemini-embedding-2', 768, 'chunker-v1', 'rag-qa-v1', 'COSINE', 'NONE', 'BUILDING')",
				indexVersionId, "IT-HYBRID-" + UUID.randomUUID(), modelConfigId);
		String unitVector = "[" + "0.5,".repeat(767) + "0.5]";
		jdbcTemplate.update(
				"INSERT INTO ai.knowledge_embeddings (chunk_id, index_version_id, embedding, embedding_model, embedding_dimension, document_version) VALUES (?, ?, CAST(? AS vector), 'gemini-embedding-2', 768, 2)",
				chunkV2, indexVersionId, unitVector);
		jdbcTemplate.update(
				"INSERT INTO ai.knowledge_embeddings (chunk_id, index_version_id, embedding, embedding_model, embedding_dimension, document_version) VALUES (?, ?, CAST(? AS vector), 'gemini-embedding-2', 768, 1)",
				tenantChunk, indexVersionId, unitVector);
		jdbcTemplate.update(
				"INSERT INTO ai.knowledge_embeddings (chunk_id, index_version_id, embedding, embedding_model, embedding_dimension, document_version) VALUES (?, ?, CAST(? AS vector), 'gemini-embedding-2', 768, 1)",
				otherTenantChunk, indexVersionId, unitVector);
		List<HybridSearchRow> rows = knowledgeChunkRepository.hybridSearch(
				null, indexVersionId, unitVector, "quy trinh dang ky the",
				List.of("PUBLIC"), 20, 20, 0.15, 0.01, 60, 0.6, 0.4, 3, 5);

		org.junit.jupiter.api.Assertions.assertEquals(1, rows.size());
		org.junit.jupiter.api.Assertions.assertEquals(chunkV2, rows.get(0).chunkId());
		org.junit.jupiter.api.Assertions.assertEquals(1, rows.get(0).lexicalRank());
		org.junit.jupiter.api.Assertions.assertEquals(1, rows.get(0).vectorRank());

		List<HybridSearchRow> tenantRows = knowledgeChunkRepository.hybridSearch(
				targetTenant, indexVersionId, unitVector, "quy trinh dang ky the",
				List.of("PUBLIC", "TENANT_PRIVATE"), 20, 20, 0.15, 0.01, 60, 0.6, 0.4, 3, 5);
		org.junit.jupiter.api.Assertions.assertTrue(
				tenantRows.stream().anyMatch(row -> row.chunkId().equals(tenantChunk)));
		org.junit.jupiter.api.Assertions.assertTrue(
				tenantRows.stream().noneMatch(row -> row.chunkId().equals(otherTenantChunk)));
	}

	@Test
	@Transactional
	void retrievalAuditAndCitationsPersistWithUniqueCitationConstraint() {
		UUID sourceId = UUID.randomUUID();
		jdbcTemplate.update(
				"INSERT INTO ai.knowledge_sources (source_id, title, access_scope, status) VALUES (?, ?, 'PUBLIC', 'ACTIVE')",
				sourceId, "Citation IT source");
		UUID documentId = UUID.randomUUID();
		insertReadyDocument(sourceId, UUID.randomUUID(), documentId, 1, null);
		UUID chunkId = insertReadyChunk(documentId, 1, null, "nội dung trích dẫn kiểm thử");
		UUID conversationId = UUID.randomUUID();
		UUID messageId = UUID.randomUUID();
		jdbcTemplate.update(
				"INSERT INTO operations.chat_conversations (conversation_id, conversation_type, status, title) VALUES (?, 'SYSTEM_DIRECT', 'ACTIVE', 'IT')",
				conversationId);
		jdbcTemplate.update(
				"INSERT INTO operations.chat_messages (message_id, conversation_id, message_type, content) VALUES (?, ?, 'ASSISTANT_TEXT', 'Trả lời kiểm thử')",
				messageId, conversationId);

		RetrievalAudit audit = new RetrievalAudit(
				UUID.randomUUID(), null, conversationId, null, null, null, null,
				"câu hỏi", "hash", null, "retrieval-policy-v1", "retrieval-threshold-v1", "PUBLIC",
				0, 1, 1, 1, 5, 3, new java.math.BigDecimal("0.900"), null, 12L,
				Instant.now(),
				List.of(new RetrievalAudit.RetrievalAuditResult(
						documentId, chunkId, null, 1, null, new java.math.BigDecimal("0.500"),
						new java.math.BigDecimal("0.01000"), 1, true)));
		RetrievalAudit savedAudit = retrievalAuditPersistenceAdapter.save(audit);
		org.junit.jupiter.api.Assertions.assertEquals(1, savedAudit.results().size());
		org.junit.jupiter.api.Assertions.assertTrue(
				retrievalAuditPersistenceAdapter.findById(savedAudit.retrievalAuditId()).isPresent());

		AiMessageCitation citation = new AiMessageCitation(
				UUID.randomUUID(), messageId, documentId, chunkId, "C1", "Tiêu đề", 1, "Mục 1",
				new java.math.BigDecimal("0.01000"), savedAudit.retrievalAuditId(), null, 0);
		List<AiMessageCitation> saved = aiMessageCitationPersistenceAdapter.saveAll(messageId, List.of(citation));
		org.junit.jupiter.api.Assertions.assertEquals(1, saved.size());
		org.junit.jupiter.api.Assertions.assertEquals("C1", saved.getFirst().label());
		org.junit.jupiter.api.Assertions.assertEquals(1,
				aiMessageCitationPersistenceAdapter.findByMessageId(messageId).size());
		AiMessageCitation duplicate = new AiMessageCitation(
				UUID.randomUUID(), messageId, documentId, chunkId, "C1", "Tiêu đề", 1, "Mục 1",
				new java.math.BigDecimal("0.01000"), savedAudit.retrievalAuditId(), null, 0);
		org.junit.jupiter.api.Assertions.assertThrows(
				org.springframework.dao.DataIntegrityViolationException.class,
				() -> aiMessageCitationPersistenceAdapter.saveAll(messageId, List.of(duplicate)));
	}

	private void insertReadyDocument(UUID sourceId, UUID documentKey, UUID documentId, int version, String content) {
		jdbcTemplate.update(
				"INSERT INTO ai.knowledge_documents (document_id, source_id, document_key, title, access_scope, status, document_version) VALUES (?, ?, ?, ?, 'PUBLIC', 'READY', ?)",
				documentId, sourceId, documentKey, "Tài liệu IT", version);
	}

	private UUID insertReadyChunk(UUID documentId, int version, UUID tenantId, String content) {
		UUID chunkId = UUID.randomUUID();
		jdbcTemplate.update(
				"INSERT INTO ai.knowledge_chunks (chunk_id, document_id, tenant_id, title, content, chunk_index, access_scope, document_version, status) VALUES (?, ?, ?, ?, ?, 0, 'PUBLIC', ?, 'READY')",
				chunkId, documentId, tenantId, "Tiêu đề", content, version);
		return chunkId;
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
			org.junit.jupiter.api.Assertions.assertNotNull(flyway.info().current());
			org.junit.jupiter.api.Assertions.assertEquals(0, flyway.info().pending().length);
			UUID ticketId = UUID.randomUUID();
			try (Connection fresh = DriverManager.getConnection(
					freshUrl, hikariDataSource.getUsername(), hikariDataSource.getPassword());
				 Statement statement = fresh.createStatement()) {
				try (var resultSet = statement.executeQuery(
						"SELECT COUNT(*) FROM ai.ai_model_configurations WHERE use_case = 'SUPPORT_CHAT' AND status = 'ACTIVE'"
				)) {
					org.junit.jupiter.api.Assertions.assertTrue(resultSet.next());
					org.junit.jupiter.api.Assertions.assertEquals(1, resultSet.getInt(1));
				}
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


