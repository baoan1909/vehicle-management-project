package com.ban.vehicle_management.ai.golden;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.ai.service.*;
import com.ban.vehicle_management.application.notification.notification.port.out.NotificationRealtimeEventPublisherPortOut;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatRealtimeEventPublisherPortOut;
import com.ban.vehicle_management.application.operations.guestsupport.model.CaptchaVerificationResult;
import com.ban.vehicle_management.application.operations.guestsupport.port.out.PublicChallengeVerificationPort;
import com.ban.vehicle_management.infrastructure.ai.lmstudio.LmStudioProperties;
import com.ban.vehicle_management.infrastructure.persistence.adapter.ai.AiRuntimeModePersistenceAdapter;
import com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.security.*;
import java.security.interfaces.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/** Real HTTP/controller/security/SQL/worker/local-inference evaluation; opt-in only. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "server.address=127.0.0.1", "spring.jpa.show-sql=false", "logging.level.org.hibernate.SQL=OFF",
    "app.ai.assistant-enabled=false", "app.ai.allow-unpaid-support-data=true", "app.ai.assistant-job-concurrency=1",
    "app.ai.embedding.enabled=true", "app.ai.cache.enabled=false", "app.ai.ingestion.enabled=false",
    "app.ai.lm-studio.context-tokens=2048", "app.ai.runtime-switch-initial-delay-ms=3600000",
    "app.ai.model-catalog-sync-initial-delay-ms=3600000", "app.ai.embedding.build-initial-delay-ms=3600000",
    "app.ai.gemini.base-url=http://127.0.0.1:9", "app.ai.gemini.api-key=isolated-disabled-google-fixture",
    "app.mail.enabled=false", "app.rabbitmq.listener-enabled=false", "app.ocr.enabled=false",
    "app.public-support.capability-secret=isolated-api-fixture-capability-secret-never-for-production",
    "app.public-support.captcha-enabled=true", "app.public-support.captcha-required-for-assistant=true",
    "app.public-support.captcha-site-key=qwen-fixture-site-key",
    "app.public-support.captcha-expected-hostnames[0]=qwen-fixture.invalid",
    "app.public-support.rate-window=1s", "app.public-support.rate-limit-local-fallback-enabled=false",
    "app.redis.password=", "app.redis.username=", "spring.data.redis.password=", "spring.data.redis.username="
})
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "AI_QWEN_API_LIVE", matches = "true")
class QwenChatApiAcceptanceIT extends PostgresIntegrationTestSupport {
    private static final Path OUT = Path.of(System.getenv().getOrDefault("AI_QWEN_API_OUTPUT", "target/qwen-api-live"));
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4.1-alpine")).withExposedPorts(6379);
    static { REDIS.start(); }

    @DynamicPropertySource static void isolatedRedis(DynamicPropertyRegistry registry) {
        registry.add("app.redis.host", REDIS::getHost);
        registry.add("app.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("app.redis.environment", () -> "qwen-api-isolated");
        registry.add("app.redis.service", () -> "run-" + REDIS.getContainerId());
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired Environment environment;
    @Autowired EmbeddingService embeddings;
    @Autowired EmbeddingPromptFormatter formatter;
    @Autowired LmStudioProperties local;
    @Autowired AiRuntimeModePersistenceAdapter mode;
    @Autowired AiAssistantProperties settings;
    @LocalServerPort int port;
    // Only external identity/challenge delivery and outbound realtime transports are substituted.
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean PublicChallengeVerificationPort challenge;
    @MockitoBean ChatRealtimeEventPublisherPortOut realtime;
    @MockitoBean NotificationRealtimeEventPublisherPortOut notificationRealtime;

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    private final List<Map<String, Object>> observations = new ArrayList<>();
    private NimbusJwtEncoder encoder;
    private String base;

    @Test void evaluatePublishedChatAnswersThroughActualHttpApis() throws Exception {
        Files.createDirectories(OUT);
        assertFalse(Files.exists(OUT.resolve("api-observations.json")), "Use a fresh output directory; never overwrite a failed cohort");
        var rows = loadCases();
        String dataset = System.getenv().getOrDefault("AI_QWEN_API_DATASET", "all");
        int offset = Integer.parseInt(System.getenv().getOrDefault("AI_QWEN_API_OFFSET", "0"));
        int limit = Integer.parseInt(System.getenv().getOrDefault("AI_QWEN_API_LIMIT", "6"));
        var selected = QwenApiCaseSelection.select(rows, dataset, offset, limit);
        configureExternalIdentityAndCaptcha();
        base = "http://127.0.0.1:" + port + environment.getProperty("server.servlet.context-path", "");
        assertEquals(2048, local.getContextTokens());
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("kind", "REAL_CHATBOX_HTTP_API_WITH_ISOLATED_DB_AND_REAL_LOCAL_MODELS");
        metadata.put("baseUrl", base); metadata.put("contextTokens", local.getContextTokens());
        metadata.put("chatModel", local.getChatModel()); metadata.put("embeddingModel", local.getEmbeddingModel());
        metadata.put("availableCases", rows.size()); metadata.put("setupCompleted", false);
        metadata.put("harnessSha256", QwenApiCorpusFixture.hash(Files.readString(Path.of("src/test/java/com/ban/vehicle_management/ai/golden/QwenChatApiAcceptanceIT.java"))));
        metadata.put("manifestSha256", QwenApiCorpusFixture.hash(Files.readString(QwenApiCorpusFixture.AUTHORING.resolve("knowledge-manifest.json"))));
        metadata.put("selection", Map.of("dataset", dataset, "offset", offset, "limit", limit,
                "caseIds", selected.stream().map(row -> row.path("caseId").asText()).toList()));
        metadata.put("identity", "Signed ephemeral RSA JWT; fixture verifier; real DB permission/ownership guards");
        metadata.put("substitutions", List.of("external identity issuer", "external CAPTCHA verification", "outbound realtime delivery"));
        metadata.put("rateLimit", "Production limiter + isolated Redis,1second fixture window; not production quota acceptance");
        metadata.put("sourceApproval", "Simulated only in disposable DB; authoring stays DRAFT");
        metadata.put("qualityAccepted", false);
        json.writerWithDefaultPrettyPrinter().writeValue(OUT.resolve("metadata.json").toFile(), metadata);
        var corpus = new QwenApiCorpusFixture(jdbc, embeddings, formatter, local, json).seed(OUT, mode);
        metadata.put("corpus", corpus); metadata.put("setupCompleted", true);
        json.writerWithDefaultPrettyPrinter().writeValue(OUT.resolve("metadata.json").toFile(), metadata);
        settings.setAssistantEnabled(true); settings.setAllowUnpaidSupportData(true);
        verifyHttpAuthorizationBoundaries();
        int failures = 0;
        try {
            for (var row : selected) {
                var observation = runCase(row);
                observations.add(observation);
                json.writerWithDefaultPrettyPrinter().writeValue(OUT.resolve("api-observations.json").toFile(), observations);
                assertEquals(0, googleCalls(), "Disabled provider must have zero HTTP, including catalog");
                failures = Boolean.TRUE.equals(observation.get("infrastructureFailure")) ? failures + 1 : 0;
                if (failures >= 2) {
                    json.writerWithDefaultPrettyPrinter().writeValue(OUT.resolve("aborted.json").toFile(),
                            Map.of("reason", "Two consecutive infrastructure failures", "recorded", observations.size(), "planned", selected.size()));
                    fail("Infrastructure unavailable; preserve failures and do not claim quality acceptance");
                }
            }
            int holdSeconds = Integer.parseInt(System.getenv().getOrDefault("AI_QWEN_API_BROWSER_HOLD_SECONDS", "0"));
            if (holdSeconds < 0 || holdSeconds > 300) throw new IllegalArgumentException("Browser hold must be0..300seconds");
            if (holdSeconds > 0) {
                json.writerWithDefaultPrettyPrinter().writeValue(OUT.resolve("browser-ready.json").toFile(),
                        Map.of("baseUrl", base, "expiresAt", Instant.now().plusSeconds(holdSeconds).toString(),
                                "captchaExternalVerifierIsFixture", true));
                long deadline = System.nanoTime() + Duration.ofSeconds(holdSeconds).toNanos();
                while (System.nanoTime() < deadline && !Files.exists(OUT.resolve("browser-complete.json"))) Thread.sleep(250);
            }
        } finally { settings.setAssistantEnabled(false); }
        assertEquals(selected.size(), observations.size());
        // Semantic correctness is separately adjudicated from these actual API transcripts.
    }

    private List<JsonNode> loadCases() throws Exception {
        List<JsonNode> rows = new ArrayList<>();
        var manifest = json.readTree(QwenApiCorpusFixture.AUTHORING.resolve("knowledge-manifest.json").toFile());
        for (var dataset : manifest.path("datasets")) {
            var path = QwenApiCorpusFixture.AUTHORING.resolve("evaluation").resolve(dataset.path("file").asText());
            String name = "baseline".equals(dataset.path("kind").asText()) ? "baseline"
                    : "batch-%02d".formatted(dataset.path("batch").asInt());
            for (String line : Files.readAllLines(path)) if (!line.isBlank()) {
                var row = (ObjectNode) json.readTree(line); row.put("evaluationDataset", name); rows.add(row);
            }
        }
        assertEquals(1104, rows.size(), "Do not drop baseline or old batch cases");
        Path newFile = QwenApiCorpusFixture.AUTHORING.resolve("evaluation/qwen-api-new-evaluation.jsonl");
        var frozen = json.readTree(newFile.resolveSibling("qwen-api-new-evaluation-metadata.json").toFile());
        assertEquals(frozen.path("sha256").asText(), QwenApiCorpusFixture.hash(Files.readString(newFile)));
        Path references = newFile.resolveSibling("qwen-api-new-references.json");
        assertEquals(frozen.path("referenceSha256").asText(), QwenApiCorpusFixture.hash(Files.readString(references)), "Reference drift before API evaluation");
        for (String line : Files.readAllLines(newFile)) if (!line.isBlank()) {
            var row = (ObjectNode) json.readTree(line); row.put("evaluationDataset", "new"); rows.add(row);
        }
        return rows;
    }

    private void configureExternalIdentityAndCaptcha() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        var key = new com.nimbusds.jose.jwk.RSAKey.Builder((RSAPublicKey) pair.getPublic()).privateKey((RSAPrivateKey) pair.getPrivate()).keyID("ephemeral-api-fixture").build();
        encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
        var verifier = NimbusJwtDecoder.withPublicKey((RSAPublicKey) pair.getPublic()).build();
        verifier.setJwtValidator(JwtValidators.createDefaultWithIssuer("qwen-api-fixture"));
        when(jwtDecoder.decode(anyString())).thenAnswer(call -> verifier.decode(call.getArgument(0)));
        when(challenge.verify(anyString(), anyString())).thenAnswer(call -> new CaptchaVerificationResult(
                true, "support_assistant", "qwen-fixture.invalid", Instant.now(), null));
    }

    private record Actor(UUID account, UUID customer, String token, Set<String> permissions, Map<String, Object> fixtureData) { }

    private void verifyHttpAuthorizationBoundaries() throws Exception {
        var owner = actor(true); var other = actor(false);
        long callsBefore = count("ai.provider_http_calls");
        var opened = request("GET", "/api/operations/support-tickets/assistant-conversation", null, owner.token());
        assertEquals(200, opened.status());
        String conversation = opened.body().path("data").path("conversationId").asText();
        assertFalse(conversation.isBlank());
        String path = "/api/operations/chat/conversations/" + conversation + "/messages";
        var own = request("GET", path, null, owner.token());
        var unauthorized = request("GET", path, null, null);
        var forbidden = request("GET", path, null, other.token());
        var unauthenticatedSend = request("POST", path, Map.of("content", "Không được gửi khi chưa đăng nhập"), null);
        var invalidSignature = request("GET", path, null, owner.token().substring(0, owner.token().lastIndexOf('.') + 1) + "invalid");
        var evidence = Map.of("ownerRead", own.status(), "guestRead", unauthorized.status(),
                "otherAccountRead", forbidden.status(), "guestSend", unauthenticatedSend.status(),
                "invalidSignature", invalidSignature.status(), "modelHttpDelta", count("ai.provider_http_calls") - callsBefore);
        json.writerWithDefaultPrettyPrinter().writeValue(OUT.resolve("http-authorization-boundaries.json").toFile(), evidence);
        assertEquals(200, own.status()); assertEquals(401, unauthorized.status());
        assertTrue(Set.of(403, 404).contains(forbidden.status()), "Other account must not read conversation");
        assertEquals(401, unauthenticatedSend.status()); assertEquals(401, invalidSignature.status());
        assertEquals(callsBefore, count("ai.provider_http_calls"), "Authorization rejection must not invoke a model");
    }

    private Actor actor(boolean business) {
        UUID role = UUID.randomUUID(), profile = UUID.randomUUID(), account = UUID.randomUUID(), customer = UUID.randomUUID();
        String code = "API_" + account.toString().substring(0, 8).toUpperCase(Locale.ROOT);
        jdbc.update("insert into iam.roles(role_id,code,name,is_system,is_active,created_at) values (?,?,?,false,true,now())", role, code, code);
        Set<String> permissions = new LinkedHashSet<>(List.of("SUPPORT_WIDGET_ACCESS_OWN", "CHAT_CONVERSATION_READ_OWN", "CHAT_MESSAGE_SEND_OWN", "SUPPORT_TICKET_READ_OWN"));
        if (business) permissions.addAll(List.of("AI_KNOWLEDGE_READ_CUSTOMER", "CUSTOMER_VEHICLE_READ_OWN", "SUBSCRIPTION_READ_OWN", "PARKING_SESSION_READ_OWN", "SUPPORT_TICKET_CREATE_OWN", "SUPPORT_TICKET_CATEGORY_CUSTOMER_OWN"));
        for (String permission : permissions) assertEquals(1, jdbc.update("insert into iam.role_permissions(id,role_id,permission_id,created_at,is_active,is_system) select ?,?,permission_id,now(),true,false from iam.permissions where permission_code=?", UUID.randomUUID(), role, permission), "Unknown permission fixture");
        jdbc.update("insert into people.user_profiles(user_profile_id,full_name,status,created_at) values (?,?,'ACTIVE',now())", profile, "API Fixture " + code);
        jdbc.update("insert into people.customers(customer_id,user_profile_id,customer_code,customer_type,status,approval_status,created_at) values (?,?,?,'REGISTERED','ACTIVE','APPROVED',now())", customer, profile, "CUS-" + account);
        jdbc.update("insert into iam.accounts(account_id,user_profile_id,username,email,keycloak_user_id,role_id,status,failed_login_count,created_at) values (?,?,?,?,?,?,'ACTIVE',0,now())", account, profile, "api-" + account, "api-" + account + "@example.invalid", "fixture-" + account, role);
        UUID vehicleType = jdbc.queryForObject("select vehicle_type_id from catalog.vehicle_types where is_active=true order by code limit 1", UUID.class);
        UUID vehicle = UUID.randomUUID();
        String plate = "59A" + account.toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        jdbc.update("insert into people.customer_vehicles(customer_vehicle_id,customer_id,vehicle_type_id,license_plate,brand,color,is_default,status,created_at) values (?,?,?,?,'Fixture brand','Fixture blue',true,'ACTIVE',now())", vehicle, customer, vehicleType, plate);
        UUID lot = UUID.randomUUID(), subscription = UUID.randomUUID(), ticket = UUID.randomUUID();
        jdbc.update("insert into parking.parking_lots(parking_lot_id,code,name,total_capacity,status) values (?,?,?,10,'ACTIVE')", lot, "API-" + account, "API Fixture Lot " + code);
        UUID ticketType = jdbc.queryForObject("select ticket_type_id from catalog.ticket_types order by code limit 1", UUID.class);
        var today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        jdbc.update("insert into access_control.subscriptions(subscription_id,customer_id,customer_vehicle_id,ticket_type_id,parking_lot_id,effective_from,effective_to,price,status,period_quantity,physical_card_requested,physical_card_deposit,physical_card_status) values (?,?,?,?,?,?,?,40000,'PENDING_PAYMENT',1,false,0,'NOT_REQUESTED')", subscription, customer, vehicle, ticketType, lot, today, today.plusDays(29));
        UUID category = jdbc.queryForObject("select category_id from operations.support_ticket_categories where status='ACTIVE' and audience_type='CUSTOMER' order by display_order,category_id limit 1", UUID.class);
        String ticketTitle = "Fixture support " + code;
        jdbc.update("insert into operations.support_tickets(support_ticket_id,requester_account_id,requester_role_code,requester_name_snapshot,category_id,title,content,status,source) values (?,?,?,?,?,?,?,'OPEN','SELF_SERVICE_PORTAL')", ticket, account, code, "API Fixture " + code, category, ticketTitle, "Synthetic local API fixture; no external delivery");
        Map<String, Object> fixture = Map.of("ownerAccount", account, "ownerCustomer", customer,
                "vehicleId", vehicle, "vehiclePlateSynthetic", plate, "subscriptionId", subscription,
                "subscriptionStatus", "PENDING_PAYMENT", "subscriptionPrice", 40000,
                "supportTicketId", ticket, "supportTicketTitle", ticketTitle,
                "supportTicketStatus", "OPEN");
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("qwen-api-fixture").subject("fixture-" + account)
                .issuedAt(now).expiresAt(now.plusSeconds(3600)).claim("account_id", account.toString())
                .claim("preferred_username", "api-" + account).build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue();
        return new Actor(account, customer, token, Set.copyOf(permissions), fixture);
    }

    private Map<String, Object> runCase(JsonNode row) throws Exception {
        var record = new LinkedHashMap<String, Object>(); record.put("case", row);
        record.put("semanticGrade", "PENDING_REVIEW"); record.put("actualIntent", null);
        record.put("actualIntentNote", "Not inferred from expected labels; review published response and persisted runtime evidence");
        String persona = row.path("persona").asText();
        boolean guest = "PUBLIC".equals(persona) || (persona.isBlank() && "PUBLIC".equals(row.path("expectedAccessScope").asText()));
        Actor actor = guest ? null : actor(!"NO_BUSINESS_PERMISSION".equals(persona));
        if (actor != null) record.put("actor", Map.of("account", actor.account(), "customer", actor.customer(), "effectiveAssignedPermissions", actor.permissions()));
        List<Map<String, Object>> turns = new ArrayList<>();
        String session = "api-" + UUID.randomUUID(); String context = null;
        String conversation = null;
        if (!guest) {
            var opened = request("GET", "/api/operations/support-tickets/assistant-conversation", null, actor.token());
            conversation = opened.body().path("data").path("conversationId").asText();
            if (opened.status() != 200 || conversation.isBlank()) {
                record.put("failure", "OPEN_ASSISTANT_HTTP_" + opened.status()); record.put("response", opened.body());
                record.put("infrastructureFailure", opened.status() >= 500); return record;
            }
        }
        for (var previous : row.path("context")) {
            var turn = guest ? guestTurn(previous.asText(), session, context) : memberTurn(previous.asText(), conversation, actor);
            turns.add(turn); context = previous.asText();
            if (!Boolean.TRUE.equals(turn.get("transportCompleted"))) { record.put("contextSetupFailed", true); break; }
        }
        long started = System.nanoTime();
        long auditBefore = count("ai.ai_retrieval_audits"), httpBefore = count("ai.provider_http_calls"), ticketBefore = count("operations.support_tickets");
        try {
            var turn = guest ? guestTurn(row.path("question").asText(), session, context)
                    : memberTurn(row.path("question").asText(), conversation, actor);
            record.putAll(turn);
        } catch (Exception failure) {
            record.put("failure", failure.getClass().getSimpleName());
            record.put("infrastructureFailure", true); // no raw exception message containing credentials/URLs
        }
        record.put("contextTurns", turns); record.put("elapsedMs", (System.nanoTime() - started) / 1_000_000);
        record.put("retrievalAudits", jdbc.queryForList("select retrieval_audit_id,retrieval_method,diagnostic_code,access_scopes,source_snapshot,grounded_confidence,latency_ms,input_message_id,output_message_id from ai.ai_retrieval_audits order by created_at,retrieval_audit_id offset ?", auditBefore));
        record.put("modelHttp", jdbc.queryForList("select provider,operation,http_status,latency_ms from ai.provider_http_calls order by started_at,call_id offset ?", httpBefore));
        record.put("sourceEvidence", jdbc.queryForList("""
                select r.retrieval_audit_id,r.document_id,r.chunk_id,r.final_rank,r.selected_for_context,
                       d.document_version,d.title,d.access_scope as document_scope,d.tenant_id as document_tenant,
                       d.reviewed_at,d.reviewed_by,d.effective_from,d.effective_to,d.checksum_sha256,
                       c.access_scope as chunk_scope,c.tenant_id as chunk_tenant,c.source_section,c.content,
                       s.access_scope as source_scope,s.tenant_id as source_tenant,s.status as source_status
                from ai.ai_retrieval_audit_results r
                join ai.knowledge_documents d on d.document_id=r.document_id
                join ai.knowledge_chunks c on c.chunk_id=r.chunk_id
                join ai.knowledge_sources s on s.source_id=d.source_id
                where r.retrieval_audit_id in
                  (select retrieval_audit_id from ai.ai_retrieval_audits order by created_at,retrieval_audit_id offset ?)
                order by r.retrieval_audit_id,r.final_rank
                """, auditBefore));
        record.put("supportTicketCountDelta", count("operations.support_tickets") - ticketBefore);
        record.put("noUnconfirmedTicketCreated", count("operations.support_tickets") == ticketBefore);
        record.put("fixtureExpectedData", actor == null ? Map.of("privateDataAllowed", false)
                : Map.of("rows", actor.fixtureData(), "vehicles", 1, "subscriptions", 1,
                        "parkingHistory", 0, "supportTicketsBeforeCase", 1,
                        "note", "Positive vehicle/subscription/support fixtures; history is empty and does not prove populated-history correctness"));
        return record;
    }

    private Map<String, Object> guestTurn(String question, String session, String context) throws Exception {
        var captcha = request("POST", "/api/public/support/captcha/verify", Map.of("purpose", "ASSISTANT", "challengeToken", "fixture-" + UUID.randomUUID(), "sessionId", session), null);
        String proof = captcha.body().path("data").path("proof").asText();
        var body = new LinkedHashMap<String, Object>(); body.put("message", question); body.put("sessionId", session);
        body.put("captchaProof", proof); if (context != null) body.put("contextQuestion", context);
        var response = request("POST", "/api/public/support/assistant/messages", body, null);
        Map<String, Object> record = new LinkedHashMap<>(); record.put("endpoint", "/api/public/support/assistant/messages");
        record.put("httpStatus", response.status()); record.put("response", response.body());
        record.put("answer", response.body().path("data").path("answer").asText());
        record.put("transportCompleted", response.status() == 200); record.put("infrastructureFailure", response.status() >= 500);
        record.put("diagnosticCode", response.body().path("data").path("diagnosticCode").asText());
        return record;
    }

    private Map<String, Object> memberTurn(String question, String conversation, Actor actor) throws Exception {
        String endpoint = "/api/operations/chat/conversations/" + conversation + "/messages";
        var sent = request("POST", endpoint, Map.of("content", question), actor.token());
        Map<String, Object> record = new LinkedHashMap<>(); record.put("endpoint", endpoint); record.put("sendHttpStatus", sent.status());
        String input = sent.body().path("data").path("messageId").asText(); record.put("inputMessageId", input);
        if (sent.status() != 201 || input.isBlank()) {
            record.put("response", sent.body()); record.put("transportCompleted", false);
            record.put("infrastructureFailure", sent.status() >= 500); return record;
        }
        long deadline = System.nanoTime() + Duration.ofSeconds(100).toNanos();
        JsonNode status = json.createObjectNode();
        while (System.nanoTime() < deadline) {
            var polled = request("GET", "/api/ai/assistant/messages/" + input + "/status", null, actor.token());
            status = polled.body();
            if (polled.status() != 200 || status.path("data").path("terminal").asBoolean()) break;
            Thread.sleep(300);
        }
        var history = request("GET", endpoint, null, actor.token());
        List<JsonNode> outputs = new ArrayList<>();
        for (var message : history.body().path("data")) {
            if (input.equals(message.path("replyToMessageId").asText()) && message.path("senderAccountId").isNull()) outputs.add(message);
        }
        record.put("jobStatusResponse", status); record.put("publishedMessages", outputs);
        record.put("answer", String.join("\n", outputs.stream().map(message -> message.path("content").asText()).toList()));
        record.put("transportCompleted", status.path("data").path("terminal").asBoolean() && !outputs.isEmpty());
        record.put("infrastructureFailure", !status.path("data").path("terminal").asBoolean());
        record.put("runs", jdbc.queryForList("select run_id,provider,model_id,status,latency_ms,failure_code,input_tokens,output_tokens from ai.ai_runs where input_message_id=? order by created_at", UUID.fromString(input)));
        record.put("toolCalls", jdbc.queryForList("select tool_call_id,tool_name,tool_type,status,confirmed_at,executed_at,failure_code,response_payload_redacted from ai.ai_tool_calls where input_message_id=? order by created_at", UUID.fromString(input)));
        return record;
    }

    private record Response(int status, JsonNode body) { }

    private Response request(String method, String path, Object body, String token) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(100));
        builder.header("Content-Type", "application/json");
        if (token != null) builder.header("Authorization", "Bearer " + token);
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        var response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        JsonNode payload;
        if (response.body().isBlank()) payload = json.createObjectNode();
        else {
            try { payload = json.readTree(response.body()); }
            catch (com.fasterxml.jackson.core.JsonProcessingException notJson) {
                payload = json.createObjectNode().put("nonJsonResponse", true);
            }
        }
        return new Response(response.statusCode(), payload);
    }

    private long count(String table) {
        if (!Set.of("ai.ai_retrieval_audits", "ai.provider_http_calls", "operations.support_tickets").contains(table)) throw new IllegalArgumentException("Test table allowlist");
        return jdbc.queryForObject("select count(*) from " + table, Long.class);
    }

    private int googleCalls() { return jdbc.queryForObject("select count(*) from ai.provider_http_calls where provider='GEMINI'", Integer.class); }

    @AfterEach void retainEvidenceOnFailure() throws Exception {
        settings.setAssistantEnabled(false); Files.createDirectories(OUT);
        json.writerWithDefaultPrettyPrinter().writeValue(OUT.resolve("http-calls.json").toFile(),
                jdbc.queryForList("select provider,operation,http_status,count(*) as calls from ai.provider_http_calls group by provider,operation,http_status"));
        assertEquals(0, googleCalls());
    }

    @AfterAll static void stopOwnedRedis() { REDIS.stop(); }
}
