package com.ban.vehicle_management.infrastructure.ai.gemini;

import static org.junit.jupiter.api.Assertions.*;

import com.ban.vehicle_management.application.ai.service.AiAssistantProperties;
import com.ban.vehicle_management.domain.ai.model.*;
import com.ban.vehicle_management.shared.enumeration.ai.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

class GeminiCredentialPoolTest {
    private static final String OK = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"ok\"}]}}]}";
    private static final String DAILY = "{\"error\":{\"status\":\"RESOURCE_EXHAUSTED\",\"details\":[{\"violations\":[{\"quotaId\":\"GenerateRequestsPerDayPerProjectPerModel-FreeTier\"}]}]}}";
    private static final String AUTH = "{\"error\":{\"status\":\"INVALID_ARGUMENT\",\"details\":[{\"reason\":\"API_KEY_INVALID\"}]}}";
    private final List<String> calls = new CopyOnWriteArrayList<>();
    private HttpServer server;
    private ExecutorService executor;
    private GeminiProperties properties;
    private MockEnvironment environment;
    private BiFunction<String, String, Reply> responder;
    private GeminiCredentialPool pool;
    private GeminiAiProviderAdapter adapter;
    private final ObjectMapper json = new ObjectMapper();
    private record Reply(int status, String body) { }

    @BeforeEach void setup() throws Exception {
        properties = new GeminiProperties();
        properties.setApiKey("synthetic-primary"); properties.setProjectId("real-project-alpha");
        properties.setGenerationMinInterval(Duration.ZERO);
        properties.setEmbeddingMinInterval(Duration.ZERO);
        properties.setCallBudget(Duration.ofSeconds(3));
        environment = new MockEnvironment();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newVirtualThreadPerTaskExecutor(); server.setExecutor(executor);
        responder = (key, path) -> new Reply(200, OK);
        server.createContext("/", exchange -> {
            String key = exchange.getRequestHeaders().getFirst("x-goog-api-key");
            String path = exchange.getRequestURI().getPath();
            exchange.getRequestBody().readAllBytes();
            calls.add(key + ":" + path);
            Reply reply = responder.apply(key, path);
            byte[] body = reply.body().getBytes(StandardCharsets.UTF_8);
            try { exchange.sendResponseHeaders(reply.status(), body.length); exchange.getResponseBody().write(body); }
            finally { exchange.close(); }
        });
        server.start(); properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
    }
    @AfterEach void stop() { server.stop(0); executor.close(); }
    private void backup(int index, String project) {
        environment.withProperty("GEMINI_API_KEY_" + index, "synthetic-backup-" + index)
                .withProperty("GEMINI_PROJECT_ID_" + index, project);
    }
    private void initialize() {
        pool = new GeminiCredentialPool(properties, environment, null);
        adapter = new GeminiAiProviderAdapter(properties, new AiAssistantProperties(),
                new GeminiRequestMapper(json), new GeminiResponseMapper(json), pool);
    }
    private AiModelConfiguration model(String id) {
        var model = new AiModelConfiguration(); model.setProvider(AiProvider.GEMINI);
        model.setUseCase(AiUseCase.SUPPORT_CHAT); model.setModelId(id); model.setFreeTierApproved(true);
        return model;
    }
    private AiResponse generate(String... names) {
        var models = Arrays.stream(names).map(this::model).toList();
        AiResponse result = null;
        for (var attempt : adapter.generationAttempts(models)) {
            result = adapter.generateAttempt(new AiRequest("test", List.of(), false, null,
                    List.of(), List.of(), List.of()), attempt);
            if (result.success() || result.safetyBlocked() || !result.retryable()) return result;
        }
        return result;
    }

    @Test void primarySuccessNeverTouchesBackup() {
        backup(1, "real-project-beta"); initialize();
        assertTrue(generate("model-a", "model-b").success());
        assertTrue(generate("model-a", "model-b").success());
        assertEquals(2, calls.size()); assertTrue(calls.stream().allMatch(item -> item.startsWith("synthetic-primary:")));
    }
    @Test void exhaustsModelsOnCurrentKeyBeforeChangingCredentialAndRemembersModel() {
        backup(1, "real-project-beta");
        responder = (key, path) -> path.contains("model-a:") ? new Reply(429, DAILY) : new Reply(200, OK);
        initialize(); assertTrue(generate("model-a", "model-b").success());
        assertTrue(generate("model-a", "model-b").success());
        assertEquals(3, calls.size()); assertTrue(calls.stream().allMatch(item -> item.startsWith("synthetic-primary:")));
        assertTrue(calls.getLast().contains("model-b:"));
    }
    @Test void invalidKeyIsNotRetriedForEveryModelAndBackupStaysSelected() {
        backup(1, "real-project-beta");
        responder = (key, path) -> key.equals("synthetic-primary") ? new Reply(400, AUTH) : new Reply(200, OK);
        initialize(); assertTrue(generate("model-a", "model-b").success());
        assertTrue(generate("model-a", "model-b").success());
        assertEquals(1, calls.stream().filter(item -> item.startsWith("synthetic-primary:")).count());
        assertTrue(calls.getLast().startsWith("synthetic-backup-1:"));
        assertEquals("AUTH", pool.diagnostics().stream().filter(item -> item.variable().equals("GEMINI_API_KEY")).findFirst().orElseThrow().status());
    }
    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"API key not valid. Please pass a valid API key.",
            "API key expired. Please renew the API key.", "Your API key was reported as leaked. Please use another API key."})
    void legacyAuthenticationBodyWithoutErrorInfoStillSkipsOnlyTheBadCredential(String message) throws Exception {
        backup(1, "real-project-beta");
        String body = json.writeValueAsString(Map.of("error", Map.of("status", "INVALID_ARGUMENT", "message", message)));
        responder = (key, path) -> key.equals("synthetic-primary") ? new Reply(message.startsWith("Your") ? 403 : 400, body) : new Reply(200, OK);
        initialize(); assertTrue(generate("model-a", "model-b").success());
        assertEquals(1, calls.stream().filter(item -> item.startsWith("synthetic-primary:")).count());
        assertEquals("AUTH", pool.failureEvents().getFirst().kind());
    }
    @Test void dailyQuotaSkipsSiblingKeysInSameProjectButNotAnotherProject() {
        backup(1, "real-project-alpha"); backup(2, "real-project-beta");
        responder = (key, path) -> key.equals("synthetic-primary") ? new Reply(429, DAILY) : new Reply(200, OK);
        initialize(); assertTrue(generate("model-a").success());
        assertEquals(2, calls.size()); assertTrue(calls.getLast().startsWith("synthetic-backup-2:"));
    }
    @Test void modelPermissionDoesNotInvalidateKeyForAnotherModel() {
        backup(1, "real-project-beta");
        responder = (key, path) -> path.contains("model-a:") ? new Reply(403, "{}") : new Reply(200, OK);
        initialize(); assertTrue(generate("model-a", "model-b").success());
        assertEquals(2, calls.size()); assertTrue(calls.getLast().startsWith("synthetic-primary:"));
    }
    @Test void badRequestDoesNotScanOtherModelsOrKeys() {
        backup(1, "real-project-beta"); responder = (key, path) -> new Reply(400, "{}"); initialize();
        assertEquals("HTTP_400", generate("model-a", "model-b").failureCode()); assertEquals(1, calls.size());
    }
    @Test void locallyInvalidRequestDoesNotSendOrRetireAnyCredential() {
        backup(1, "real-project-beta"); initialize();
        assertEquals("GEMINI_REQUEST_BUILD_FAILED", generate("invalid model", "model-b").failureCode());
        assertTrue(calls.isEmpty()); assertTrue(pool.failureEvents().isEmpty());
        assertTrue(pool.diagnostics().stream().allMatch(item -> item.calls() == 0));
    }
    @Test void safetyBlockNeverSwitchesModel() {
        backup(1, "real-project-beta");
        responder = (key, path) -> new Reply(200, "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}"); initialize();
        assertTrue(generate("model-a", "model-b").safetyBlocked()); assertEquals(1, calls.size());
    }
    @ParameterizedTest @ValueSource(ints = {408, 429, 500, 503})
    void temporaryFailureCanUseNextModelWithoutChangingKey(int status) {
        backup(1, "real-project-beta");
        responder = (key, path) -> path.contains("model-a:") ? new Reply(status, "{}") : new Reply(200, OK);
        initialize(); assertTrue(generate("model-a", "model-b").success());
        assertEquals(2, calls.size()); assertTrue(calls.getLast().startsWith("synthetic-primary:"));
    }
    @Test void moreThanFiveCredentialsAndSparseIndexesAreSupported() {
        for (int i : List.of(1, 3, 5, 7, 9, 12)) backup(i, "real-project-" + i);
        responder = (key, path) -> key.equals("synthetic-backup-12") ? new Reply(200, OK) : new Reply(401, AUTH);
        initialize(); assertEquals(7, pool.credentials().size()); assertTrue(generate("model-a").success());
        assertEquals(7, calls.size());
    }
    @Test void placeholdersAndDuplicatesAreExcludedWithoutDisclosure() {
        properties.setApiKey("KEY_CHINH");
        environment.withProperty("GEMINI_API_KEY_1", "KEY_DU_PHONG_1");
        backup(2, "real-project-beta"); environment.withProperty("GEMINI_API_KEY_4", "synthetic-backup-2");
        initialize(); assertEquals(1, pool.credentials().size());
        String diagnostics = pool.diagnostics().toString();
        assertTrue(diagnostics.contains("PLACEHOLDER")); assertTrue(diagnostics.contains("DUPLICATE"));
        assertFalse(diagnostics.contains("synthetic-backup-2"));
        assertFalse(pool.credentials().toString().contains("synthetic-backup-2"));
    }
    @Test void rawProviderEchoOfAnyConfiguredKeyIsRedactedBeforeMapping() {
        backup(1, "real-project-beta");
        responder = (key, path) -> new Reply(200, OK.replace("ok", "synthetic-primary synthetic-backup-1"));
        initialize(); AiResponse result = generate("model-a");
        assertTrue(result.success()); assertFalse(result.rawBody().contains("synthetic-"));
    }
    @Test void maximumAttemptsCapsWireCallsAndDoesNotMeanAllKeysWereTested() {
        properties.setMaxAttempts(2); backup(1, "real-project-beta"); backup(2, "real-project-gamma");
        responder = (key, path) -> new Reply(401, AUTH); initialize(); generate("model-a");
        assertEquals(2, calls.size());
        assertEquals("UNTESTED", pool.diagnostics().stream().filter(item -> item.variable().equals("GEMINI_API_KEY_2")).findFirst().orElseThrow().status());
    }
    @Test void expiredPlanCannotMakeAnHttpRequest() {
        initialize(); AiResponse result = adapter.generateAttempt(new AiRequest("test", List.of(), false, null,
                List.of(), List.of(), List.of()), new AiProviderAttempt(model("model-a"), "primary", Instant.EPOCH));
        assertFalse(result.success()); assertTrue(calls.isEmpty());
    }
    @Test void shortRemainingBudgetDoesNotFalselyDisableTheNextModel() {
        backup(1, "real-project-beta"); properties.setCallBudget(Duration.ofMillis(400));
        var secondCalls = new java.util.concurrent.atomic.AtomicInteger();
        responder = (key, path) -> {
            try { Thread.sleep(path.contains("model-a:") || secondCalls.incrementAndGet() == 1 ? 350 : 10); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            return new Reply(200, OK);
        };
        initialize();
        var deadlines = new AiAssistantProperties(); deadlines.setGenerationTimeout(Duration.ofMillis(250));
        adapter = new GeminiAiProviderAdapter(properties, deadlines, new GeminiRequestMapper(json), new GeminiResponseMapper(json), pool);
        assertFalse(generate("model-a", "model-b").success());
        assertTrue(pool.failureEvents().stream().noneMatch(event -> event.model().equals("model-b")));
        assertTrue(generate("model-a", "model-b").success());
        assertTrue(calls.getLast().startsWith("synthetic-primary:"));
    }
    @Test void concurrentRequestsDoNotKeepProbingAnInvalidCredential() throws Exception {
        backup(1, "real-project-beta");
        responder = (key, path) -> key.equals("synthetic-primary") ? new Reply(401, AUTH) : new Reply(200, OK);
        initialize();
        try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<AiResponse>> results = new ArrayList<>();
            for (int i = 0; i < 8; i++) results.add(workers.submit(() -> generate("model-a")));
            for (var result : results) assertTrue(result.get(5, TimeUnit.SECONDS).success());
        }
        assertEquals(1, calls.stream().filter(item -> item.startsWith("synthetic-primary:")).count());
    }
    @Test void dailyEmbeddingQuotaDoesNotDisableGenerationOnTheSameCredential() throws Exception {
        initialize(); var credential = pool.credentials().getFirst();
        assertEquals(GeminiCredentialPool.Failure.DAILY_QUOTA, pool.failure(credential,
                GeminiCredentialPool.EMBEDDING, "embedding-a", 429, DAILY, null));
        assertNull(pool.acquire(credential, GeminiCredentialPool.EMBEDDING, "embedding-a", pool.deadline()));
        assertTrue(generate("model-a").success());
    }
    @Test void cooldownAndFailureReportsSerializeWithTheExistingPlainJacksonMapper() throws Exception {
        initialize(); var credential = pool.credentials().getFirst();
        pool.failure(credential, GeminiCredentialPool.GENERATION, "model-a", 429, DAILY, null);
        assertDoesNotThrow(() -> json.writeValueAsString(pool.failureEvents()));
        var state = new GeminiCredentialPool.Availability("2026-11-21T08:00:00Z", "DAILY_QUOTA");
        assertEquals(state, json.readValue(json.writeValueAsString(state), GeminiCredentialPool.Availability.class));
    }
    @Test void embeddingChangesCredentialWithoutChangingModelOrDimension() {
        backup(1, "real-project-beta");
        responder = (key, path) -> key.equals("synthetic-primary") ? new Reply(429, DAILY)
                : new Reply(200, "{\"embedding\":{\"values\":[" + String.join(",", Collections.nCopies(768, "0.1")) + "]}}");
        initialize();
        var embeddings = new GeminiEmbeddingProviderAdapter(properties, new AiAssistantProperties(),
                new GeminiEmbeddingRequestMapper(json), new GeminiEmbeddingResponseMapper(json), pool);
        var configuration = model("gemini-embedding-2"); configuration.setOutputDimension(768);
        var result = embeddings.embed(new EmbeddingRequest("task: question answering | query: test", null), configuration);
        assertTrue(result.isSuccess()); assertEquals(768, result.getVector().dimension());
        assertEquals(2, calls.size());
        assertTrue(calls.stream().allMatch(call -> call.endsWith("/gemini-embedding-2:embedContent")));
    }
    @Test void invalidEmbeddingDimensionNeverFallsBackToAnotherModelOrCredential() {
        backup(1, "real-project-beta");
        responder = (key, path) -> new Reply(200, "{\"embedding\":{\"values\":[0.1,0.2]}}");
        initialize();
        var embeddings = new GeminiEmbeddingProviderAdapter(properties, new AiAssistantProperties(),
                new GeminiEmbeddingRequestMapper(json), new GeminiEmbeddingResponseMapper(json), pool);
        var configuration = model("gemini-embedding-2"); configuration.setOutputDimension(768);
        assertEquals("EMBEDDING_DIMENSION_MISMATCH", embeddings.embed(new EmbeddingRequest("test", null), configuration).getFailureCode());
        assertEquals(1, calls.size());
    }
    @Test void catalogDoesNotRetryAnInvalidKeyOnEveryRefresh() {
        backup(1, "real-project-beta");
        responder = (key, path) -> key.equals("synthetic-primary") ? new Reply(401, AUTH)
                : new Reply(200, "{\"models\":[{\"name\":\"models/model-a\",\"supportedGenerationMethods\":[\"generateContent\"]}]}");
        initialize(); assertEquals(1, adapter.listModels().size()); assertEquals(1, adapter.listModels().size());
        assertEquals(1, calls.stream().filter(item -> item.startsWith("synthetic-primary:")).count());
    }
    @Test void retryAfterHttpDateAndDailyResetUseProviderTime() throws Exception {
        Clock fixed = Clock.fixed(Instant.parse("2026-11-20T07:59:00Z"), ZoneOffset.UTC);
        pool = new GeminiCredentialPool(properties, environment, null, fixed);
        var credential = pool.credentials().getFirst();
        pool.failure(credential, GeminiCredentialPool.GENERATION, "model-a", 429, DAILY, "1");
        assertNull(pool.acquire(credential, GeminiCredentialPool.GENERATION, "model-a", fixed.instant().plusSeconds(5)));
        assertEquals(GeminiCredentialPool.Failure.RATE_LIMIT, pool.failure(credential,
                GeminiCredentialPool.GENERATION, "model-b", 429, "{}", "Fri, 20 Nov 2026 08:01:00 GMT"));
        assertNull(pool.acquire(credential, GeminiCredentialPool.GENERATION, "model-b", fixed.instant().plusSeconds(5)));
    }

    @Test void nearFullDeadlineTimeoutCoolsDownPairAndKeepsNextModelAvailable() {
        properties.setCallBudget(Duration.ofMillis(990));
        responder = (key, path) -> {
            if (path.contains("model-a:")) {
                try { Thread.sleep(1400); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            }
            return new Reply(200, OK);
        };
        initialize();
        var deadlines = new AiAssistantProperties(); deadlines.setGenerationTimeout(Duration.ofMillis(1000));
        adapter = new GeminiAiProviderAdapter(properties, deadlines, new GeminiRequestMapper(json), new GeminiResponseMapper(json), pool);
        var first = adapter.generationAttempts(List.of(model("model-a"), model("model-b"))).getFirst();
        var result = adapter.generateAttempt(new AiRequest("test", List.of(), false, null,
                List.of(), List.of(), List.of()), first);
        assertEquals("TIMEOUT", result.failureCode());
        assertTrue(pool.failureEvents().stream().anyMatch(event -> event.model().equals("model-a")));
        assertTrue(generate("model-a", "model-b").success());
        assertEquals(1, calls.stream().filter(call -> call.contains("model-a:")).count());
    }

}
