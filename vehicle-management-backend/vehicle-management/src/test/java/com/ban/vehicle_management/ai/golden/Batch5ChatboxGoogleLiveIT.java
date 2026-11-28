package com.ban.vehicle_management.ai.golden;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.ai.port.out.KnowledgeDocumentStoragePort;
import com.ban.vehicle_management.application.ai.service.*;
import com.ban.vehicle_management.infrastructure.ai.gemini.*;
import com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport;
import com.fasterxml.jackson.databind.*;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Real HTTP, SQL authorization, ingestion, indexing, FAQ and assistant orchestration.
 * Identity verification uses a test JwtDecoder; private file storage replaces MinIO only.
 * No provider/router/retrieval/business port is mocked. No automatic semantic PASS. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.ai.assistant-enabled=true", "app.ai.allow-unpaid-support-data=true",
        "app.ai.assistant-job-concurrency=1", "app.ai.ingestion.claim-limit=1",
        "app.ai.ingestion.enabled=true", "app.ai.embedding.enabled=true",
        "app.ai.cache.enabled=false", "app.redis.enabled=false", "app.rabbitmq.listener-enabled=false"})
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "BATCH5_GOOGLE_LIVE", matches = "true")
@Import(Batch5ChatboxGoogleLiveIT.StorageConfiguration.class)
class Batch5ChatboxGoogleLiveIT extends PostgresIntegrationTestSupport {
    private static final Path PHASE = Path.of("docs/ai-knowledge/phase-0/authoring");
    private static final Path OUTPUT = Path.of(System.getenv().getOrDefault("BATCH5_OUTPUT", "target/batch05-google-live"));
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();
    private static final List<Map<String, Object>> WIRE = new CopyOnWriteArrayList<>();
    private static final Map<String, String> ALIASES = new HashMap<>();
    private static final java.util.concurrent.atomic.AtomicLong WIRE_SEQUENCE = new java.util.concurrent.atomic.AtomicLong();
    private static HttpServer proxy;
    private static volatile String currentCase = "BOOTSTRAP";
    private final List<Map<String, Object>> transcript = new ArrayList<>();
    private final List<Map<String, Object>> ingestionTranscript = new ArrayList<>();
    private final Map<UUID, String> documentSlugs = new HashMap<>();
    private final Map<String, String> identities = new LinkedHashMap<>();
    private final Map<String, UUID> accountIds = new HashMap<>();
    private final Set<String> resumedCompleted = new HashSet<>();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private int lastApiStatus;
    @LocalServerPort int port;
    @Value("${server.servlet.context-path:}") String contextPath;
    @Autowired JdbcTemplate jdbc;
    @Autowired KnowledgeIngestionOrchestrator ingestion;
    @Autowired AssistantOrchestrator assistant;
    @Autowired KnowledgeIndexBuildService indexBuild;
    @Autowired GeminiCredentialPool credentials;
    @Autowired GeminiProperties geminiProperties;
    @Autowired GeminiAiProviderAdapter provider;
    @Autowired AiModelRouter router;
    @Autowired AiAssistantProperties assistantProperties;
    @Autowired EmbeddingPromptFormatter embeddingPrompt;
    @Autowired com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatConversationPortOut chatStorage;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean TaskScheduler taskScheduler;

    @DynamicPropertySource
    static void googleProxy(DynamicPropertyRegistry properties) throws IOException {
        proxy = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        proxy.setExecutor(java.util.concurrent.Executors.newCachedThreadPool(task -> {
            Thread thread = new Thread(task, "batch5-google-observer"); thread.setDaemon(true); return thread;
        }));
        HttpClient upstream = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        proxy.createContext("/", exchange -> {
            long started = System.nanoTime();
            String route = exchange.getRequestURI().getPath();
            String secret = exchange.getRequestHeaders().getFirst("x-goog-api-key");
            Map<String, Object> attempt = new java.util.concurrent.ConcurrentHashMap<>();
            attempt.put("caseId", currentCase); attempt.put("sequence", WIRE_SEQUENCE.incrementAndGet());
            attempt.put("credentialVariable", ALIASES.getOrDefault(secret, "UNMAPPED_CONFIGURED_CREDENTIAL"));
            attempt.put("path", route); attempt.put("startedAt", Instant.now().toString());
            try {
                if (!route.matches("/v1(?:beta)?/models(?:/[A-Za-z0-9._:-]+)?")) throw new IOException("Unsupported Google API path");
                var request = HttpRequest.newBuilder(URI.create("https://generativelanguage.googleapis.com" + exchange.getRequestURI()))
                        .timeout(Duration.ofSeconds(25)).header("x-goog-api-key", secret);
                byte[] body = exchange.getRequestBody().readAllBytes();
                request.method(exchange.getRequestMethod(), body.length == 0 ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(body));
                if (body.length > 0) request.header("Content-Type", "application/json");
                attempt.put("observationStatus", "UPSTREAM_SEND_STARTED");
                WIRE.add(attempt); write("wire-attempts.json", WIRE);
                var response = upstream.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
                attempt.put("httpStatus", response.statusCode());
                attempt.put("observationStatus", "UPSTREAM_RESPONSE_OBSERVED");
                if (response.statusCode() >= 400) {
                    JsonNode error = JSON.readTree(response.body()).path("error");
                    attempt.put("providerErrorCode", error.path("code").asText());
                    attempt.put("providerErrorStatus", error.path("status").asText());
                    List<Map<String, String>> violations = new ArrayList<>();
                    for (JsonNode detail : error.path("details")) {
                        if (detail.has("retryDelay")) attempt.put("retryInfoDelay", detail.path("retryDelay").asText());
                        for (JsonNode violation : detail.path("violations")) {
                            Map<String, String> quota = new LinkedHashMap<>();
                            for (String field : List.of("quotaMetric", "quotaId", "quotaValue")) if (violation.has(field)) quota.put(field, violation.path(field).asText());
                            if (!quota.isEmpty()) violations.add(quota);
                        }
                    }
                    attempt.put("quotaViolations", violations);
                }
                attempt.put("retryAfter", response.headers().firstValue("Retry-After").orElse(""));
                response.headers().firstValue("Retry-After").ifPresent(v -> exchange.getResponseHeaders().add("Retry-After", v));
                exchange.sendResponseHeaders(response.statusCode(), response.body().length);
                exchange.getResponseBody().write(response.body());
            } catch (Exception failure) {
                attempt.put("transportFailure", failure.getClass().getSimpleName());
                attempt.put("observationStatus", "TRANSPORT_FAILURE");
                byte[] message = "{\"error\":{\"message\":\"Live upstream transport failed\"}}".getBytes(StandardCharsets.UTF_8);
                try { exchange.sendResponseHeaders(503, message.length); exchange.getResponseBody().write(message); } catch (IOException ignored) { }
            } finally {
                attempt.put("latencyMs", (System.nanoTime() - started) / 1_000_000);
                try { write("wire-attempts.json", WIRE); } catch (IOException ignored) { }
                exchange.close();
            }
        });
        proxy.start();
        properties.add("app.ai.gemini.base-url", () -> "http://127.0.0.1:" + proxy.getAddress().getPort());
    }

    @Test
    void uploadReviewAndEvaluateWithCheckpointsOnFreshDatabase() throws Exception {
        Files.createDirectories(OUTPUT);
        credentials.credentials().forEach(c -> ALIASES.put(c.secret(), c.variable()));
        String stop = null;
        try {
            assertFalse(ALIASES.isEmpty(), "No configured Google credential; never substitute offline results");
            String admin = actor("ADMIN", Set.of("AI_KNOWLEDGE_READ_ALL", "AI_KNOWLEDGE_MANAGE_ALL", "AI_KNOWLEDGE_APPROVE_ALL", "AI_KNOWLEDGE_REINDEX_ALL"));
            identities.put("PUBLIC", actor("PUBLIC", Set.of()));
            identities.put("CUSTOMER", actor("CUSTOMER", Set.of("AI_KNOWLEDGE_READ_CUSTOMER")));
            identities.put("EMPLOYEE", actor("EMPLOYEE", Set.of("AI_KNOWLEDGE_READ_EMPLOYEE")));
            verifyApiAuthorization();
            JsonNode manifest = JSON.readTree(PHASE.resolve("knowledge-manifest.json").toFile());
            write("cohort.json", Map.of("manifestSha256", sha(Files.readAllBytes(PHASE.resolve("knowledge-manifest.json"))),
                    "holdoutSha256", sha(Files.readAllBytes(PHASE.resolve("evaluation/batch-05-holdout.jsonl"))),
                    "runtimeSha256", Map.of("router", classSha(com.ban.vehicle_management.domain.ai.model.AssistantIntentRouter.class),
                            "businessPolicy", classSha(com.ban.vehicle_management.domain.ai.model.AssistantBusinessRequestPolicy.class),
                            "orchestrator", classSha(AssistantOrchestrator.class), "indexUseCase", classSha(KnowledgeIndexVersionUseCaseImpl.class), "harness", classSha(Batch5ChatboxGoogleLiveIT.class)),
                    "embeddingProfile", jdbc.queryForMap("select provider,model_id,api_version,output_dimension from ai.ai_model_configurations where use_case='EMBEDDING' and status='ACTIVE' order by priority limit 1"),
                    "embeddingPromptVersion", embeddingPrompt.version(), "assistantPromptVersion", assistantProperties.getPromptVersion(),
                    "timeouts", Map.of("generationTimeoutMs", assistantProperties.getGenerationTimeout().toMillis(), "providerCallBudgetMs", geminiProperties.getCallBudget().toMillis(), "requestTimeoutMs", assistantProperties.getRequestTimeout().toMillis()),
                    "testBoundary", "Real socket HTTP and DB permissions; test JWT verifier and private filesystem storage; one manual worker",
                    "semanticAcceptance", false));
            // Use the migration-seeded approved registry. Catalog presence never grants free-tier approval.
            var configurations = router.resolveCandidates(com.ban.vehicle_management.shared.enumeration.ai.AiUseCase.SUPPORT_CHAT);
            write("approved-model-registry.json", configurations.stream().map(configuration -> {
                Map<String, Object> snapshot = new LinkedHashMap<>();
                snapshot.put("configurationId", configuration.getConfigurationId()); snapshot.put("modelId", configuration.getModelId());
                snapshot.put("useCase", configuration.getUseCase().name()); snapshot.put("status", configuration.getStatus().name());
                snapshot.put("priority", configuration.getPriority()); snapshot.put("rolloutPercentage", configuration.getRolloutPercentage());
                snapshot.put("apiVersion", configuration.getApiVersion()); snapshot.put("freeTierApproved", configuration.isFreeTierApproved());
                snapshot.put("maxOutputTokens", configuration.getMaxOutputTokens()); snapshot.put("temperature", configuration.getTemperature());
                return snapshot;
            }).toList());
            String resume = System.getenv("BATCH5_RESUME_FROM");
            Set<String> completed = resumedCompleted;
            if (resume != null && !resume.isBlank()) {
                Path prior = Path.of(resume);
                if (!JSON.readTree(OUTPUT.resolve("cohort.json").toFile()).equals(JSON.readTree(prior.resolve("cohort.json").toFile()))
                        || !registryPolicy(JSON.readTree(OUTPUT.resolve("approved-model-registry.json").toFile())).equals(registryPolicy(JSON.readTree(prior.resolve("approved-model-registry.json").toFile()))))
                    throw new IllegalStateException("RESUME_PROVENANCE_CHANGED_START_NEW_COHORT_WITHOUT_INHERITING_COMPLETION");
                JSON.readTree(prior.resolve("checkpoint.json").toFile()).path("caseIdsCompleted").forEach(id -> completed.add(id.asText()));
            }
            currentCase = "CATALOG_PREFLIGHT";
            var catalog = provider.listModels();
            write("model-catalog.json", catalog);
            write("catalog-preflight.json", Map.of("approvedCandidates", configurations.size(), "catalogModels", catalog.size(),
                    "note", "Catalog visibility is not generation quota, quality or project billing evidence"));
            if (catalog.isEmpty()) throw new IllegalStateException("LIVE_CATALOG_UNAVAILABLE");
            if (configurations.stream().noneMatch(c -> catalog.stream().anyMatch(m -> m.modelId().equals(c.getModelId()) && m.supportedActions().contains("generateContent"))))
                throw new IllegalStateException("NO_CATALOG_SUPPORTED_APPROVED_GENERATION_MODEL");
            // Import batch 5 first, then all historical sources. No READY flag is inserted by this harness.
            List<JsonNode> documents = new ArrayList<>(); manifest.path("documents").forEach(documents::add);
            documents.sort(Comparator.comparingInt(d -> d.path("batch").asInt() == 5 ? 0 : 1));
            for (JsonNode doc : documents) importDocument(doc, admin);
            buildIndex(admin);
            List<JsonNode> cases = new ArrayList<>();
            appendCases(cases, "batch-05-questions.jsonl"); appendCases(cases, "batch-05-holdout.jsonl");
            int limit = Integer.parseInt(System.getenv().getOrDefault("BATCH5_CASE_LIMIT", "280"));
            if ("true".equals(System.getenv("BATCH5_FULL_REGRESSION"))) {
                for (JsonNode dataset : manifest.path("datasets")) if (dataset.path("batch").asInt() != 5) appendCases(cases, dataset.path("file").asText());
                appendCases(cases, "additional-evaluation.jsonl");
            }
            if ("true".equals(System.getenv("BATCH5_SMOKE"))) {
                List<JsonNode> smoke = new ArrayList<>();
                for (String topic : List.of("REGISTRATION", "WORKFORCE", "TOPOLOGY", "CATALOG"))
                    cases.stream().filter(r -> topic.equals(r.path("topic").asText())).limit(2).forEach(smoke::add);
                cases = smoke;
            }
            cases.removeIf(row -> completed.contains(row.path("caseId").asText()));
            String selectedFile = System.getenv("BATCH5_CASE_IDS_FILE");
            if (selectedFile != null && !selectedFile.isBlank()) {
                Set<String> selected = new HashSet<>(Files.readAllLines(Path.of(selectedFile)));
                cases.removeIf(row -> !selected.contains(row.path("caseId").asText()));
                if (cases.size() != selected.size()) throw new IllegalStateException("SELECTED_CASE_IDS_MISSING_DUPLICATED_OR_ALREADY_COMPLETED");
            }
            for (JsonNode row : cases.subList(0, Math.min(limit, cases.size()))) evaluate(row);
            List<JsonNode> probes = new ArrayList<>();
            appendCases(probes, "batch-05-generation-probes.jsonl");
            for (JsonNode row : probes) if (!completed.contains(row.path("caseId").asText())) evaluate(row);
        } catch (Exception | AssertionError failure) {
            stop = failure.getClass().getSimpleName() + ": " + credentials.sanitize(String.valueOf(failure.getMessage()));
        } finally {
            proxy.stop(30); // Drain outstanding observer calls before freezing exact attempt/checkpoint counts.
            write("transcript.json", transcript); write("ingestion.json", ingestionTranscript);
            write("wire-attempts.json", WIRE); write("credential-diagnostics.json", credentials.diagnostics());
            write("credential-failures.json", credentials.failureEvents());
            Map<String, Object> checkpoint = new LinkedHashMap<>();
            checkpoint.put("status", stop == null ? "RUN_FINISHED_NOT_GRADED" : "STOPPED");
            checkpoint.put("stopReason", stop); checkpoint.put("lastStageOrCase", currentCase);
            List<String> completedThisRun = transcript.stream().filter(x -> "COMPLETED_UNGRADED".equals(x.get("executionStatus"))).map(x -> String.valueOf(x.get("caseId"))).toList();
            Set<String> allCompleted = new TreeSet<>(resumedCompleted); allCompleted.addAll(completedThisRun);
            checkpoint.put("caseIdsCompleted", allCompleted); checkpoint.put("caseIdsCompletedThisRun", completedThisRun);
            checkpoint.put("inheritedCaseIdsCompleted", resumedCompleted);
            checkpoint.put("actualHttpRequests", WIRE.size()); checkpoint.put("reviewer", "PENDING_INDEPENDENT_SEMANTIC_REVIEW");
            checkpoint.put("notAttempted", "All cases absent from caseIdsCompleted; no quota balance inferred");
            checkpoint.put("database", "Testcontainers fresh vehicle_management_test; no shared DB writes");
            checkpoint.put("recordedAt", Instant.now().toString()); write("checkpoint.json", checkpoint);
        }
        assertNull(stop, "Live stopped; inspect checkpoint, no acceptance inferred: " + stop);
    }

    private void importDocument(JsonNode doc, String admin) throws Exception {
        currentCase = "INGEST:" + doc.path("slug").asText();
        Map<String, Object> observation = new LinkedHashMap<>(); observation.put("slug", doc.path("slug").asText());
        ingestionTranscript.add(observation);
        UUID source = UUID.fromString(api("POST", "/api/ai/knowledge/sources", admin,
                Map.of("title", "Isolated batch 5 " + doc.path("slug").asText(), "accessScope", doc.path("accessScope").asText())).path("data").path("sourceId").asText());
        observation.put("sourceCreated", true);
        String boundary = "batch5-" + UUID.randomUUID();
        byte[] content = Files.readAllBytes(Path.of("docs/ai-knowledge/phase-0/knowledge", doc.path("file").asText()));
        ByteArrayOutputStream multipart = new ByteArrayOutputStream();
        multipart.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"sourceId\"\r\n\r\n" + source + "\r\n--" + boundary
                + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + doc.path("file").asText()
                + "\"\r\nContent-Type: text/markdown\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        multipart.write(content); multipart.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        var response = client.send(HttpRequest.newBuilder(uri("/api/ai/knowledge/documents/upload")).header("Authorization", "Bearer " + admin)
                .header("Idempotency-Key", "batch5-upload-" + source)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary).POST(HttpRequest.BodyPublishers.ofByteArray(multipart.toByteArray())).build(), HttpResponse.BodyHandlers.ofString());
        observation.put("uploadHttpStatus", response.statusCode());
        if (response.statusCode() >= 300) observation.put("uploadFailure", credentials.sanitize(response.body()));
        requireSuccess(response.statusCode(), "UPLOAD");
        UUID document = UUID.fromString(JSON.readTree(response.body()).path("data").path("documentId").asText());
        documentSlugs.put(document, doc.path("slug").asText()); observation.put("documentId", document);
        ingestion.pollDueJobs();
        JsonNode detail = api("GET", "/api/ai/knowledge/documents/" + document, admin, null).path("data");
        observation.put("processedStatus", detail.path("status").asText()); observation.put("failureCode", detail.path("failureCode").asText());
        write("ingestion.json", ingestionTranscript); write("wire-attempts.json", WIRE);
        if (!"REVIEW".equals(detail.path("status").asText())) throw new IllegalStateException("INGESTION_NOT_REVIEW:" + detail.path("failureCode").asText() + ":" + detail.path("status").asText());
        JsonNode preview = api("GET", "/api/ai/knowledge/documents/" + document + "/chunks?documentVersion=" + detail.path("documentVersion").asInt(), admin, null);
        observation.put("previewRead", true); observation.put("previewSha256", sha(JSON.writeValueAsBytes(preview)));
        var published = api("POST", "/api/ai/knowledge/documents/" + document + "/publish", admin, Map.of());
        observation.put("publishedStatus", published.path("data").path("status").asText());
        write("ingestion.json", ingestionTranscript);
    }

    private void buildIndex(String admin) throws Exception {
        currentCase = "INDEX";
        UUID configuration = jdbc.queryForObject("select configuration_id from ai.ai_model_configurations where use_case='EMBEDDING' and status='ACTIVE' order by priority limit 1", UUID.class);
        JsonNode index = null;
        JsonNode listed = api("GET", "/api/ai/index-versions", admin, null);
        write("index-before-build.json", listed);
        for (JsonNode candidate : listed.path("data")) {
            if (Set.of("DRAFT", "BUILDING", "READY").contains(candidate.path("status").asText())) {
                if (!configuration.toString().equals(candidate.path("modelConfigurationId").asText()))
                    throw new IllegalStateException("PENDING_INDEX_CONFIGURATION_MISMATCH");
                index = candidate; break;
            }
        }
        if (index == null) index = api("POST", "/api/ai/index-versions", admin, Map.of("modelConfigurationId", configuration, "versionCode", "BATCH5_" + System.currentTimeMillis())).path("data");
        if (Set.of("DRAFT", "BUILDING").contains(index.path("status").asText())) {
            var duplicate = client.send(HttpRequest.newBuilder(uri("/api/ai/index-versions")).header("Authorization", "Bearer " + admin)
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(Map.of("modelConfigurationId", configuration, "versionCode", "DUPLICATE_BATCH5")))).build(), HttpResponse.BodyHandlers.ofString());
            write("index-duplicate-check.json", Map.of("expectedHttpStatus", 409, "actualHttpStatus", duplicate.statusCode()));
            if (duplicate.statusCode() != 409) throw new IllegalStateException("PENDING_INDEX_CONFLICT_NOT_409");
        }
        String id = index.path("indexVersionId").asText();
        if ("READY".equals(index.path("status").asText())) {
            write("index-active.json", api("POST", "/api/ai/index-versions/" + id + "/activate", admin, Map.of())); return;
        }
        if ("DRAFT".equals(index.path("status").asText())) api("POST", "/api/ai/index-versions/" + id + "/build", admin, Map.of());
        for (int i = 0; i < 1000; i++) {
            int beforeCount = jdbc.queryForObject("select embedded_chunk_count from ai.knowledge_index_versions where index_version_id=?", Integer.class, UUID.fromString(id));
            indexBuild.process(UUID.fromString(id));
            String state = jdbc.queryForObject("select status from ai.knowledge_index_versions where index_version_id=?", String.class, UUID.fromString(id));
            if ("READY".equals(state)) { write("index-active.json", api("POST", "/api/ai/index-versions/" + id + "/activate", admin, Map.of())); return; }
            if (!"BUILDING".equals(state)) throw new IllegalStateException("INDEX_NOT_READY:" + state);
            int afterCount = jdbc.queryForObject("select embedded_chunk_count from ai.knowledge_index_versions where index_version_id=?", Integer.class, UUID.fromString(id));
            if (afterCount <= beforeCount) throw new IllegalStateException("INDEX_NO_PROGRESS_OR_RETRY_PENDING:not_a_claim_that_all_keys_are_exhausted");
        }
        throw new IllegalStateException("INDEX_BOUNDED_BUDGET_EXHAUSTED");
    }

    private void evaluate(JsonNode row) throws Exception {
        currentCase = row.path("caseId").asText(); long started = System.nanoTime();
        Map<String, Object> result = new LinkedHashMap<>(); transcript.add(result);
        result.put("caseId", currentCase); result.put("question", row.path("question").asText());
        result.put("executionStatus", "RUNNING");
        result.put("actorPermissions", row.path("requiredPermissions")); result.put("gradingStatus", "NOT_GRADED"); result.put("reviewer", null);
        String scope = row.path("expectedAccessScope").isArray() ? row.path("expectedAccessScope").path(0).asText() : row.path("expectedAccessScope").asText("PUBLIC");
        Set<String> readPermissions = switch (scope) {
            case "PUBLIC" -> Set.of();
            case "CUSTOMER" -> Set.of("AI_KNOWLEDGE_READ_CUSTOMER");
            default -> Set.of("AI_KNOWLEDGE_READ_EMPLOYEE");
        };
        String token = actor("CASE", readPermissions);
        result.put("effectiveScopeFixture", scope);
        Set<String> fixturePermissions = new HashSet<>(readPermissions);
        fixturePermissions.addAll(Set.of("SUPPORT_WIDGET_ACCESS_OWN", "CHAT_CONVERSATION_READ_OWN", "CHAT_MESSAGE_SEND_OWN"));
        result.put("actualFixturePermissions", fixturePermissions);
        UUID conversation = UUID.fromString(api("GET", "/api/operations/support-tickets/assistant-conversation", token, null).path("data").path("conversationId").asText());
        int historyIndex = 0;
        for (JsonNode history : row.path("context")) {
            var previous = new com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessage();
            previous.setMessageId(UUID.randomUUID()); previous.setConversationId(conversation);
            previous.setSenderAccountId(accountIds.get(token)); previous.setMessageType(com.ban.vehicle_management.shared.enumeration.operations.ChatMessageType.TEXT);
            previous.setContent(history.asText()); previous.setCreatedAt(Instant.now().minusSeconds(60L - historyIndex++));
            chatStorage.saveMessage(previous);
        }
        result.put("historyContextApplied", true);
        result.put("historyFixture", "Persisted real chat messages before the turn under test; no provider call for fixture history");
        Map<String, String> beforeBusiness = businessSnapshot();
        // A separate synthetic fixture account per case avoids history leaking between cases.
        JsonNode posted = api("POST", "/api/operations/chat/conversations/" + conversation + "/messages", token, Map.of("content", row.path("question").asText()));
        result.put("sendHttpStatus", lastApiStatus);
        UUID message = UUID.fromString(posted.path("data").path("messageId").asText());
        var jobs = jdbc.queryForList("select job_id from ai.assistant_jobs where input_message_id=?", UUID.class, message);
        if (!jobs.isEmpty()) {
            var claimed = jdbc.queryForObject("select status from ai.assistant_jobs where job_id=?", String.class, jobs.getFirst());
            result.put("initialJobStatus", claimed);
            assistant.processDueJobs();
            Instant caseDeadline = Instant.now().plusSeconds(120);
            while (Instant.now().isBefore(caseDeadline)) {
                String state = jdbc.queryForObject("select status from ai.assistant_jobs where job_id=?", String.class, jobs.getFirst());
                result.put("jobStatus", state);
                if (Set.of("COMPLETED", "FAILED").contains(state)) break;
                if ("RETRYING".equals(state)) {
                    var due = jdbc.queryForObject("select next_attempt_at from ai.assistant_jobs where job_id=?", java.sql.Timestamp.class, jobs.getFirst());
                    result.put("nextRetryAt", due == null ? null : due.toInstant().toString());
                    write("transcript.json", transcript);
                    if (due != null && due.toInstant().isAfter(caseDeadline)) break;
                    if (due == null || !due.toInstant().isAfter(Instant.now())) assistant.processDueJobs();
                }
                Thread.sleep(250);
            }
        }
        JsonNode messages = api("GET", "/api/operations/chat/conversations/" + conversation + "/messages", token, null).path("data");
        result.put("messages", messages); result.put("responseSha256", sha(JSON.writeValueAsBytes(messages)));
        for (JsonNode output : messages) if (!output.hasNonNull("senderAccountId") && "ai".equals(output.path("relatedSchema").asText()) && message.toString().equals(output.path("replyToMessageId").asText())) {
            result.put("actualAnswer", output.path("content").asText());
            result.put("answerSha256", sha(output.path("content").asText().getBytes(StandardCharsets.UTF_8)));
            result.put("citationResponse", api("GET", "/api/ai/messages/" + output.path("messageId").asText() + "/citations", token, null));
            result.put("citationHttpStatus", lastApiStatus);
        }
        result.put("runs", jdbc.queryForList("select model_id, status, provider_status, prompt_version, failure_code, latency_ms from ai.ai_runs where input_message_id=? order by created_at", message));
        result.put("retrieval", jdbc.queryForList("select retrieval_method, access_scopes, diagnostic_code, source_snapshot, output_message_id from ai.ai_retrieval_audits where input_message_id=? order by created_at", message));
        result.put("executionStatus", "COMPLETED".equals(result.get("jobStatus")) ? "COMPLETED_UNGRADED" : "ERROR_OR_TIMEOUT");
        Map<String, String> afterBusiness = businessSnapshot();
        result.put("businessStateBeforeSha256", beforeBusiness); result.put("businessStateAfterSha256", afterBusiness);
        result.put("unexpectedBusinessMutation", !beforeBusiness.equals(afterBusiness));
        result.put("providerAttempts", WIRE.stream().filter(x -> currentCase.equals(x.get("caseId"))).toList());
        result.put("latencyMs", (System.nanoTime() - started) / 1_000_000); result.put("httpStatus", result.get("sendHttpStatus"));
        write("transcript.json", transcript); write("wire-attempts.json", WIRE);
        if (!beforeBusiness.equals(afterBusiness)) throw new IllegalStateException("UNEXPECTED_BUSINESS_MUTATION");
        if (!"COMPLETED_UNGRADED".equals(result.get("executionStatus")))
            throw new IllegalStateException("CASE_NOT_COMPLETED_CHECKPOINT:" + String.valueOf(result.get("jobStatus")) + ":not_a_claim_that_all_keys_are_exhausted");
    }

    private String actor(String kind, Set<String> additional) {
        UUID role = UUID.randomUUID(), profile = UUID.randomUUID(), account = UUID.randomUUID();
        String code = "B5_" + kind + "_" + account.toString().substring(0, 8).toUpperCase(Locale.ROOT);
        jdbc.update("insert into iam.roles(role_id,code,name,is_system,is_active) values (?,?,?,false,true)", role, code, code);
        Set<String> permissions = new HashSet<>(additional);
        permissions.addAll(Set.of("SUPPORT_WIDGET_ACCESS_OWN", "CHAT_CONVERSATION_READ_OWN", "CHAT_MESSAGE_SEND_OWN"));
        for (String permission : permissions) assertEquals(1, jdbc.update("insert into iam.role_permissions(id,role_id,permission_id,is_active,is_system) select ?,?,permission_id,true,false from iam.permissions where permission_code=?", UUID.randomUUID(), role, permission), permission);
        jdbc.update("insert into people.user_profiles(user_profile_id,full_name,status) values (?,?,'ACTIVE')", profile, "Synthetic batch 5 " + kind);
        jdbc.update("insert into iam.accounts(account_id,user_profile_id,username,email,keycloak_user_id,role_id,status,failed_login_count) values (?,?,?,?,?,?,'ACTIVE',0)", account, profile, code, code + "@example.invalid", "subject-" + account, role);
        String token = UUID.randomUUID().toString(); Instant now = Instant.now();
        when(jwtDecoder.decode(token)).thenReturn(new Jwt("fixture-token", now, now.plusSeconds(86400), Map.of("alg", "none"), Map.of("sub", "subject-" + account, "account_id", account.toString(), "preferred_username", code)));
        accountIds.put(token, account);
        return token;
    }

    private Map<String, String> businessSnapshot() throws Exception {
        Map<String, String> snapshot = new LinkedHashMap<>();
        for (String table : List.of("iam.organizations", "iam.organization_memberships", "iam.member_parking_lot_scopes", "iam.role_permissions",
                "people.employees", "parking.parking_lots", "parking.parking_levels", "parking.zones", "parking.parking_spaces",
                "catalog.vehicle_types", "catalog.ticket_types", "catalog.card_types", "catalog.price_plans", "catalog.price_rules")) {
            // Identifiers come exclusively from this fixed allowlist; only a digest leaves the DB.
            String digest = jdbc.queryForObject("select md5(coalesce(string_agg(row_to_json(item)::text, '' order by row_to_json(item)::text),'')) from " + table + " item", String.class);
            snapshot.put(table, digest);
        }
        return snapshot;
    }

    private void verifyApiAuthorization() throws Exception {
        List<Map<String, Object>> checks = new ArrayList<>();
        String scoped = actor("SCOPED", Set.of("ORGANIZATION_READ_ALL", "PARKING_SCOPE_PARTNER"));
        UUID own = UUID.randomUUID(), other = UUID.randomUUID();
        for (UUID id : List.of(own, other)) jdbc.update("insert into iam.organizations(organization_id,code,name,status) values (?,?,?,'ACTIVE')", id, "B5_" + id.toString().substring(0,8), "Synthetic isolated organization");
        jdbc.update("insert into iam.organization_memberships(organization_membership_id,organization_id,account_id,status) values (?,?,?,'ACTIVE')", UUID.randomUUID(), own, accountIds.get(scoped));
        securityCheck(checks, "OWN_ORGANIZATION", scoped, "/api/iam/organizations/" + own, 200);
        securityCheck(checks, "OTHER_ORGANIZATION", scoped, "/api/iam/organizations/" + other, 403);
        securityCheck(checks, "PUBLIC_CANNOT_MANAGE_KNOWLEDGE", identities.get("PUBLIC"), "/api/ai/knowledge/sources", 403);
        String inactive = actor("INACTIVE", Set.of("AI_KNOWLEDGE_READ_EMPLOYEE"));
        jdbc.update("update iam.accounts set status='DISABLED' where account_id=?", accountIds.get(inactive));
        securityCheck(checks, "INACTIVE_SUPPORT_ACCESS", inactive, "/api/operations/support-tickets/assistant-conversation", 403);
        write("api-security-checks.json", checks);
    }

    private void securityCheck(List<Map<String, Object>> checks, String id, String token, String route, int expected) throws Exception {
        var response = client.send(HttpRequest.newBuilder(uri(route)).header("Authorization", "Bearer " + token).GET().build(), HttpResponse.BodyHandlers.ofString());
        checks.add(Map.of("checkId", id, "expectedHttpStatus", expected, "actualHttpStatus", response.statusCode(), "passed", response.statusCode() == expected));
        write("api-security-checks.json", checks);
        if (response.statusCode() != expected) throw new IllegalStateException("API_SECURITY_FIXTURE:" + id + ":" + response.statusCode());
    }

    private JsonNode api(String method, String route, String token, Object body) throws Exception {
        var request = HttpRequest.newBuilder(uri(route)).timeout(Duration.ofSeconds(45)).header("Authorization", "Bearer " + token);
        if ("POST".equals(method)) request.header("Idempotency-Key", "batch5-" + UUID.randomUUID());
        if (body != null) request.header("Content-Type", "application/json");
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
        var response = client.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        lastApiStatus = response.statusCode();
        String redacted = credentials.sanitize(response.body());
        requireSuccess(response.statusCode(), route + ":" + redacted.substring(0, Math.min(200, redacted.length())));
        return JSON.readTree(response.body());
    }
    private URI uri(String route) { return URI.create("http://127.0.0.1:" + port + contextPath + route); }
    private static void requireSuccess(int status, String operation) { if (status < 200 || status >= 300) throw new IllegalStateException("HTTP_" + status + ":" + operation); }
    private static void appendCases(List<JsonNode> target, String file) throws IOException { for (String line : Files.readAllLines(PHASE.resolve("evaluation").resolve(file))) if (!line.isBlank()) target.add(JSON.readTree(line)); }
    private static String sha(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static String classSha(Class<?> type) throws Exception { try (InputStream stream = type.getResourceAsStream(type.getSimpleName() + ".class")) { return sha(Objects.requireNonNull(stream).readAllBytes()); } }
    private static JsonNode registryPolicy(JsonNode value) { JsonNode copy = value.deepCopy(); copy.forEach(row -> ((com.fasterxml.jackson.databind.node.ObjectNode) row).remove("configurationId")); return copy; }
    private static synchronized void write(String name, Object value) throws IOException { Files.createDirectories(OUTPUT); JSON.writerWithDefaultPrettyPrinter().writeValue(OUTPUT.resolve(name).toFile(), value); }

    @TestConfiguration(proxyBeanMethods = false)
    static class StorageConfiguration {
        @Bean @Primary KnowledgeDocumentStoragePort privateTestStorage() {
            return new KnowledgeDocumentStoragePort() {
                final Path root = OUTPUT.resolve("private-upload-objects").toAbsolutePath().normalize();
                private Path file(String key) { Path resolved = root.resolve(key).normalize(); if (!resolved.startsWith(root)) throw new IllegalArgumentException("Invalid object key"); return resolved; }
                public String store(byte[] content, String type, UUID document, String extension) { try { Files.createDirectories(root); String key = document + "." + extension; Files.write(file(key), content); return key; } catch (IOException failure) { throw new UncheckedIOException(failure); } }
                public InputStream openRead(String key) { try { return Files.newInputStream(file(key)); } catch (IOException failure) { throw new UncheckedIOException(failure); } }
                public boolean exists(String key) { return Files.exists(file(key)); }
                public long size(String key) { try { return Files.size(file(key)); } catch (IOException failure) { throw new UncheckedIOException(failure); } }
                public void delete(String key) { try { Files.deleteIfExists(file(key)); } catch (IOException failure) { throw new UncheckedIOException(failure); } }
            };
        }
    }
}
