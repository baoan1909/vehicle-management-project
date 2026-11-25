package com.ban.vehicle_management.ai.golden;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.ban.vehicle_management.application.ai.port.out.*;
import com.ban.vehicle_management.application.ai.service.*;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatConversationPortOut;
import com.ban.vehicle_management.domain.ai.model.*;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessage;
import com.ban.vehicle_management.infrastructure.ai.extraction.MarkdownDocumentExtractor;
import com.ban.vehicle_management.infrastructure.ai.gemini.GeminiAiProviderAdapter;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeEmbeddingRepository;
import com.ban.vehicle_management.shared.enumeration.ai.*;
import com.ban.vehicle_management.shared.enumeration.operations.ChatMessageType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

/** Real runtime chunker, SQL retrieval and orchestrator. Chat storage/audit are captured test ports, not an HTTP/UI E2E. */
@SpringBootTest(properties = {"app.ai.assistant-enabled=false", "app.ai.embedding.enabled=true",
        "app.ai.cache.enabled=false", "app.ai.ingestion.enabled=false"})
@ActiveProfiles("test")
@Transactional
class Phase2KnowledgeAcceptanceIT extends com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport {
    private static final Path PHASE = Path.of("docs/ai-knowledge/phase-0/authoring");
    private static final Path OUTPUT = Path.of(System.getenv().getOrDefault("AI_KNOWLEDGE_LIVE_OUTPUT", "target/chatbox-phase2"));
    private static final Path VECTOR_CACHE = Path.of("target/ai-knowledge-vector-cache/v1");
    private final List<String> datasetFiles = new ArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final Map<String, UUID> documents = new LinkedHashMap<>();
    private final Set<UUID> nonPublicDocuments = new HashSet<>();
    private final Map<UUID, String> documentScopes = new HashMap<>();
    private final Map<UUID, String> chunkEmbeddingInputs = new LinkedHashMap<>();
    private final Map<String, Map<String, Object>> timings = new LinkedHashMap<>();
    private List<AiModelConfiguration> models;
    private AiModelConfiguration embedding;
    private KnowledgeRetrievalResult lastRetrieval;
    private ChatMessage input;
    private ChatConversation conversation;
    private final List<ChatMessage> outputs = new ArrayList<>();
    private List<ChatMessage> history = List.of();
    private String caseId;

    @Autowired JdbcTemplate jdbc;
    @Autowired com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.AiToolCallRepository toolAuditRepository;
    @Autowired com.ban.vehicle_management.infrastructure.mapper.ai.AiToolCallPersistenceMapper toolAuditMapper;
    @Autowired AssistantOrchestrator orchestrator;
    @Autowired AiAssistantProperties assistantProperties;
    @Autowired com.ban.vehicle_management.infrastructure.ai.gemini.GeminiProperties geminiProperties;
    @Autowired EmbeddingService embeddings;
    @MockitoSpyBean KnowledgeRetrievalService retrieval;
    @MockitoSpyBean GeminiAiProviderAdapter generation;
    @Autowired com.ban.vehicle_management.infrastructure.ai.gemini.GeminiCredentialPool credentialPool;
    @MockitoBean ResolveQueryEmbeddingService queryEmbedding;
    @MockitoBean ChatConversationPortOut chats;
    @MockitoBean AssistantJobPortOut jobs;
    @MockitoBean AiRunPortOut runs;
    @MockitoBean AiModelRouter router;
    @MockitoBean AiModelWarningPortOut warnings;
    @MockitoBean AiMessageCitationPortOut citations;
    @MockitoBean KnowledgeRetrievalAuditPortOut audits;
    @MockitoBean KnowledgeAccessContextResolver scopes;
    @MockitoBean com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn account;
    @MockitoBean com.ban.vehicle_management.application.operations.supportticket.port.in.SupportTicketPortIn personalTickets;
    @MockitoBean com.ban.vehicle_management.application.accesscontrol.subscription.port.in.SubscriptionPortIn personalSubscriptions;
    @MockitoBean com.ban.vehicle_management.application.people.customervehicle.port.in.CustomerVehiclePortIn personalVehicles;
    @MockitoBean com.ban.vehicle_management.application.parking.parkingsession.port.in.ParkingSessionPortIn personalHistory;
    @MockitoBean AiToolCallPortOut toolCalls;

    @BeforeEach
    void corpusAndCapturedPorts() throws Exception {
        Files.createDirectories(OUTPUT);
        UUID configuration = UUID.randomUUID(), index = UUID.randomUUID();
        jdbc.update("insert into ai.ai_model_configurations(configuration_id,provider,use_case,model_id,priority,rollout_percentage,status,output_dimension) values (?,'GEMINI','EMBEDDING','gemini-embedding-2',10,100,'ACTIVE',768)", configuration);
        jdbc.update("update ai.knowledge_index_versions set status='RETIRED' where status='ACTIVE'");
        jdbc.update("insert into ai.knowledge_index_versions(index_version_id,version_code,model_configuration_id,provider,model_id,dimension,chunker_version,embedding_prompt_version,distance_metric,normalization,status,activated_at) values (?, ?, ?,'GEMINI','gemini-embedding-2',768,'runtime-test','rag-qa-v1','COSINE','L2','ACTIVE',now())", index,"batch2-"+index,configuration);
        embedding = new AiModelConfiguration(); embedding.setProvider(AiProvider.GEMINI);
        embedding.setModelId("gemini-embedding-2"); embedding.setOutputDimension(768);
        embedding.setConfigurationId(configuration); embedding.setUseCase(AiUseCase.EMBEDDING);
        JsonNode manifest = mapper.readTree(PHASE.resolve("knowledge-manifest.json").toFile());
        for (JsonNode dataset : manifest.path("datasets")) {
            if ("batch".equals(dataset.path("kind").asText())) datasetFiles.add(dataset.path("file").asText());
        }
        int position = 0;
        for (JsonNode doc : manifest.path("documents")) {
            if (doc.path("batch").asInt() == 0) continue;
            String slug = doc.path("slug").asText(), scope = doc.path("accessScope").asText();
            String authoring = Files.readString(PHASE.resolve("knowledge").resolve(slug+".md"));
            String title = authoring.lines().filter(line -> line.startsWith("title: ")).findFirst().orElseThrow().substring(7);
            UUID source = UUID.randomUUID(), document = UUID.randomUUID(); documents.put(slug,document);
            documentScopes.put(document, scope);
            if (!"PUBLIC".equals(scope)) nonPublicDocuments.add(document);
            jdbc.update("insert into ai.knowledge_sources(source_id,title,access_scope,status) values (?,?,?,'ACTIVE')",source,"ISOLATED BATCH2 "+slug,scope);
            jdbc.update("insert into ai.knowledge_documents(document_id,source_id,document_key,title,document_version,access_scope,status) values (?,?,?,?,2,?,'READY')",document,source,document,title,scope);
            var extracted = new MarkdownDocumentExtractor().extract(Files.readAllBytes(Path.of("docs/ai-knowledge/phase-0/knowledge/"+slug+".md")),"md","text/markdown");
            for (var chunk : new KnowledgeDocumentChunker().chunk(document,2,title,extracted).chunks()) {
                UUID id = UUID.randomUUID(); chunkEmbeddingInputs.put(id,"title: "+chunk.getTitle()+" | text: "+chunk.getContent());
                jdbc.update("insert into ai.knowledge_chunks(chunk_id,document_id,title,content,chunk_index,access_scope,document_version,status,source_section) values (?,?,?,?,?,?,2,'READY',?)",id,document,chunk.getTitle(),chunk.getContent(),chunk.getChunkIndex(),scope,chunk.getSourceSection());
                double[] values = new double[768]; values[position] = 1;
                jdbc.update("insert into ai.knowledge_embeddings(embedding_id,chunk_id,embedding,embedding_model,embedding_dimension,document_version,index_version_id) values (?,?,cast(? as vector),'gemini-embedding-2',768,2,?)",UUID.randomUUID(),id,KnowledgeEmbeddingRepository.toVectorLiteral(EmbeddingVector.of(values,768)),index);
            }
            position++;
        }
        assertEquals(manifest.path("documents").findValues("batch").stream()
                .filter(value -> value.asInt() > 0).count(), documents.size());
        when(queryEmbedding.embedQuery(anyString(),anyString(),any(),eq(768),anyString()))
                .thenReturn(EmbeddingResult.success(vector(4),"gemini-embedding-2"));
        when(audits.save(any())).thenAnswer(call -> call.getArgument(0));
        when(jobs.save(any())).thenAnswer(call -> call.getArgument(0));
        when(runs.save(any())).thenAnswer(call -> call.getArgument(0));
        when(citations.saveAll(any(),anyList())).thenAnswer(call -> {
            List<com.ban.vehicle_management.domain.ai.model.AiMessageCitation> values = call.getArgument(1);
            if (caseId != null && timings.containsKey(caseId)) {
                timings.get(caseId).put("citations", List.copyOf(values));
            }
            return values;
        });
        when(chats.findConversationById(any())).thenAnswer(call -> Optional.of(conversation));
        when(chats.findMessageById(any())).thenAnswer(call -> Optional.of(input));
        when(chats.findMessageHistory(any(),any(),anyInt())).thenAnswer(call -> history);
        when(chats.saveConversation(any())).thenAnswer(call -> call.getArgument(0));
        when(chats.saveMessage(any())).thenAnswer(call -> { ChatMessage value = call.getArgument(0); outputs.add(value); return value; });
        var vehicle = new com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle();
        vehicle.setLicensePlate("59A12345"); vehicle.setIsDefault(true);
        vehicle.setStatus(com.ban.vehicle_management.shared.enumeration.people.CustomerVehicleStatus.ACTIVE);
        when(personalVehicles.getMyCustomerVehicles(20)).thenReturn(List.of(vehicle));
        var subscription = new com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription();
        subscription.setStatus(com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus.ACTIVE);
        subscription.setEffectiveFrom(java.time.LocalDate.of(2026,11,1)); subscription.setEffectiveTo(java.time.LocalDate.of(2026,11,30));
        subscription.setPrice(new java.math.BigDecimal("80000")); subscription.setQrAccessToken("private-qr-never-leave");
        when(personalSubscriptions.getMySubscriptions(20)).thenReturn(List.of(subscription));
        var ticket = new com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket();
        ticket.setTitle("Không mở được màn hình"); ticket.setContent("private-content-never-leave");
        ticket.setStatus(com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus.IN_PROGRESS);
        when(personalTickets.getMyTickets(any(),any(),eq(20))).thenAnswer(call ->
                call.getArgument(0) == null || call.getArgument(0) == ticket.getStatus() ? List.of(ticket) : List.of());
        var session = new com.ban.vehicle_management.application.parking.parkingsession.model.result.ParkingSessionManagementResult(
                UUID.randomUUID(), null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null, null,
                "59A12345", null, Instant.parse("2026-11-19T01:00:00Z"), null,
                com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus.OPEN,
                new java.math.BigDecimal("8000"), List.of());
        when(personalHistory.getOwnSessionSummaries(any(),eq(20))).thenAnswer(call ->
                call.getArgument(0) == null || call.getArgument(0) == session.status() ? List.of(session) : List.of());
        when(toolCalls.save(any())).thenAnswer(call -> {
            AiToolCall value = call.getArgument(0);
            if (caseId != null && timings.containsKey(caseId)) {
                @SuppressWarnings("unchecked") List<Map<String,Object>> calls = (List<Map<String,Object>>) timings.get(caseId)
                        .computeIfAbsent("toolCalls", ignored -> new ArrayList<Map<String,Object>>());
                calls.add(Map.of("name",value.getToolName(),"status",String.valueOf(value.getStatus())));
            }
            return value;
        });
    }

    @Test
    void publicScopeCannotRetrieveAnyCustomerDocumentIncludingInvoices() {
        for (String question : List.of("thêm xe", "gia hạn vé tháng", "mất thẻ", "lịch sử gửi xe", "hóa đơn thanh toán")) {
            var result = retrieval.search(KnowledgeRetrievalContext.global(),question,List.of("PUBLIC"),5);
            assertTrue(result.results().stream().noneMatch(row -> nonPublicDocuments.contains(row.documentId())));
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named="PHASE2_LIVE_EVAL", matches="true")
    @Timeout(value = 20, unit = java.util.concurrent.TimeUnit.MINUTES)
    void bothBatchesUseRuntimeOrchestratorAndRealProvider() throws Exception {
        // Only this disposable corpus is marked READY. Authoring DRAFT and developer DB are untouched.
        for (var entry : chunkEmbeddingInputs.entrySet()) {
            String text = entry.getValue();
            var result = liveDocumentVector(text);
            jdbc.update("update ai.knowledge_embeddings set embedding=cast(? as vector) where chunk_id=?",KnowledgeEmbeddingRepository.toVectorLiteral(result),entry.getKey());
        }
        when(queryEmbedding.embedQuery(anyString(),anyString(),any(),eq(768),anyString())).thenAnswer(call -> {
            String inputText = call.getArgument(0);
            String signature = "gemini-embedding-2|768|" + call.getArgument(1) + "|" + call.getArgument(4) + "|" + inputText;
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(signature.getBytes(StandardCharsets.UTF_8)));
            Path cachedVector = VECTOR_CACHE.resolve("query-vectors").resolve(hash + ".json");
            Path previousQuery = Path.of("target/chatbox-phase2/query-vectors").resolve(hash + ".json");
            if (!Files.exists(cachedVector) && Files.exists(previousQuery)) {
                Files.createDirectories(cachedVector.getParent());
                Files.copy(previousQuery, cachedVector);
            }
            boolean replay = Boolean.parseBoolean(System.getenv().getOrDefault("PHASE2_QUERY_VECTOR_REPLAY", "false"));
            if (replay || Files.exists(cachedVector)) {
                if (!Files.exists(cachedVector)) throw new IllegalStateException("No recorded real query vector; replay never substitutes a synthetic vector or calls the provider");
                return EmbeddingResult.success(EmbeddingVector.of(mapper.readValue(cachedVector.toFile(), double[].class), 768), "gemini-embedding-2");
            }
            var result = embeddings.embed(new EmbeddingRequest(inputText, null), embedding);
            if (result.isSuccess()) {
                Files.createDirectories(cachedVector.getParent());
                mapper.writeValue(cachedVector.toFile(), result.getVector().values());
            }
            return result;
        });
        String registrySnapshot = System.getenv().getOrDefault("PHASE2_APPROVED_MODEL_SNAPSHOT", "");
        if (!registrySnapshot.isBlank()) {
            models = ApprovedModelRegistryFixture.load(Path.of(registrySnapshot), mapper);
        } else {
            // Backward-compatible legacy fixture; final evaluations supply an approved registry snapshot.
            models = new ArrayList<>();
            for (String name : System.getenv().getOrDefault("PHASE2_LIVE_MODELS","gemini-3.1-flash-lite,gemini-3.5-flash-lite").split(",")) {
                var model = new AiModelConfiguration(); model.setConfigurationId(UUID.randomUUID());
                model.setProvider(AiProvider.GEMINI); model.setUseCase(AiUseCase.SUPPORT_CHAT);
                model.setModelId(name.strip()); model.setFreeTierApproved(true); model.setMaxOutputTokens(4096);
                model.setStatus(models.isEmpty() ? com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus.ACTIVE
                        : com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus.FALLBACK);
                model.setRolloutPercentage(models.isEmpty() ? 100 : 0); model.setPriority(models.size()); models.add(model);
            }
        }
        var registryPort = org.mockito.Mockito.mock(com.ban.vehicle_management.application.ai.port.out.AiModelConfigurationPortOut.class);
        when(registryPort.findEnabledByUseCase(AiUseCase.SUPPORT_CHAT)).thenReturn(models);
        var registryRouter = new AiModelRouter(registryPort, new AiModelPolicyService(assistantProperties));
        doAnswer(call -> registryRouter.resolveCandidates(call.getArgument(0), call.getArgument(1),
                call.getArgument(2), call.getArgument(3))).when(router).resolveCandidates(any(),any(),any(),anyInt());
        doAnswer(call -> {
            timings.get(caseId).put("resolvedRetrievalQuery", call.getArgument(1).toString());
            long started = System.nanoTime(); lastRetrieval = (KnowledgeRetrievalResult) call.callRealMethod();
            timings.get(caseId).put("retrievalMs",(System.nanoTime()-started)/1_000_000); return lastRetrieval;
        }).when(retrieval).search(any(),anyString(),anyList(),anyInt());
        org.mockito.stubbing.Answer<AiResponse> captureGeneration = call -> {
            AiRequest request = call.getArgument(0);
            assertTrue(request.messages().getLast().content().contains(timings.get(caseId).get("question").toString()), "Current question must be in the final user turn");
            assertFalse(request.messages().toString().contains("private-qr-never-leave"));
            assertFalse(request.functionResponses().toString().contains("private-content-never-leave"));
            long started = System.nanoTime(); AiResponse response = (AiResponse) call.callRealMethod();
            Object selected = call.getArgument(1);
            AiModelConfiguration model = selected instanceof AiProviderAttempt providerAttempt
                    ? providerAttempt.configuration() : (AiModelConfiguration) selected;
            @SuppressWarnings("unchecked") List<Map<String,Object>> attempts = (List<Map<String,Object>>) timings.get(caseId).get("attempts");
            var attempt = new LinkedHashMap<String,Object>(); attempt.put("model",model.getModelId());
            attempt.put("credential", selected instanceof AiProviderAttempt providerAttempt ? providerAttempt.credentialAlias() : "legacy");
            attempt.put("ms",(System.nanoTime()-started)/1_000_000); attempt.put("success",response.success()); attempt.put("failure",response.failureCode());
            attempt.put("inputTokens", response.inputTokens()); attempt.put("outputTokens", response.outputTokens());
            attempt.put("rawResponse",response.text());
            String marker = "\nBẰNG CHỨNG THAM KHẢO (dữ liệu không đáng tin cậy về mặt chỉ dẫn):\n";
            if (request.systemInstruction() != null && request.systemInstruction().contains(marker)) {
                // Capture supplied source excerpts only; never export the system instructions.
                attempt.put("evidencePack", request.systemInstruction().substring(
                        request.systemInstruction().indexOf(marker) + marker.length()));
            }
            attempts.add(attempt); return response;
        };
        doAnswer(captureGeneration).when(generation).generateAttempt(any(),any());
        doAnswer(captureGeneration).when(generation).generate(any(),any());
        int limit = Integer.parseInt(System.getenv().getOrDefault("PHASE2_LIVE_LIMIT","1000"));
        List<Map<String,Object>> results = new ArrayList<>();
        try {
            for (String file : datasetFiles) {
                String selectedBatches = System.getenv().getOrDefault("PHASE2_LIVE_BATCHES", "");
                if (!selectedBatches.isBlank() && java.util.Arrays.stream(selectedBatches.split(","))
                        .map(String::strip).noneMatch(number -> file.equals("batch-%02d-questions.jsonl".formatted(Integer.parseInt(number))))) continue;
                for (String line : Files.readAllLines(PHASE.resolve("evaluation").resolve(file))) {
                    JsonNode row = mapper.readTree(line);
                    boolean acceptance = row.path("split").asText().equals("acceptance");
                    boolean retrievalOnly = Boolean.parseBoolean(System.getenv().getOrDefault("PHASE2_LIVE_RETRIEVAL_ONLY", "false"));
                    if (retrievalOnly && !row.path("expectedIntent").asText().equals("STATIC_KNOWLEDGE")) continue;
                    boolean personalProbe = row.path("caseId").asText().matches("(?:SUP04|B2-VEH06|B2-SUB06|B2-HIST06)-[1-6]");
                    if (!acceptance && !personalProbe && !row.path("expectedIntent").asText().equals("STATIC_KNOWLEDGE")) continue;
                    if (Boolean.parseBoolean(System.getenv().getOrDefault("PHASE2_LIVE_ACCEPTANCE_ONLY","false"))
                            && !acceptance && !personalProbe) continue;
                    if (Boolean.parseBoolean(System.getenv().getOrDefault("PHASE2_LIVE_SMOKE","false"))
                            && !(acceptance && row.path("caseId").asText().endsWith("-1"))) continue;
                    if (results.size() >= limit) break;
                    String selectedCases = System.getenv().getOrDefault("PHASE2_LIVE_CASES", "");
                    if (!selectedCases.isBlank() && java.util.Arrays.stream(selectedCases.split(","))
                            .map(String::strip).noneMatch(row.path("caseId").asText()::equals)) continue;
                    caseId = row.path("caseId").asText(); lastRetrieval = null; outputs.clear();
                    System.out.println("LIVE_CASE_START "+caseId);
                    var measurement = new LinkedHashMap<String,Object>(); timings.put(caseId,measurement);
                    measurement.put("attempts",new ArrayList<Map<String,Object>>());
                    measurement.put("queryVectorMode", Boolean.parseBoolean(System.getenv().getOrDefault("PHASE2_QUERY_VECTOR_REPLAY", "false")) ? "REPLAY_RECORDED_REAL_VECTOR" : "LIVE_PROVIDER_OR_RECORDED_REAL_VECTOR");
                    measurement.put("caseId",caseId); measurement.put("batch",file); measurement.put("split",row.path("split").asText());
                    measurement.put("question",row.path("question").asText()); measurement.put("expectedDocument",row.path("documentSlug").asText());
                    measurement.put("personalProbe",personalProbe);
                    List<String> granted = row.path("persona").asText().equals("PUBLIC") ? List.of("PUBLIC") : List.of("PUBLIC","CUSTOMER");
                    measurement.put("grantedScopes", granted);
                    when(scopes.resolveScopes()).thenReturn(granted);
                    long started = System.nanoTime();
                    if (!retrievalOnly && (acceptance || personalProbe)) runJob(row,measurement);
                    else {
                        List<String> previous = new ArrayList<>(); row.path("context").forEach(value -> previous.add(value.asText()));
                        var resolved = AssistantQuestionResolver.resolve(row.path("question").asText(),previous);
                        retrieval.search(KnowledgeRetrievalContext.global(),resolved.query(),granted,5);
                    }
                    measurement.put("totalMs",(System.nanoTime()-started)/1_000_000);
                    if (lastRetrieval != null) {
                        boolean scopeValid = lastRetrieval.results().stream().allMatch(found ->
                                granted.contains(documentScopes.get(found.documentId())));
                        measurement.put("retrievalScopesValid", scopeValid);
                        assertTrue(scopeValid, "Retrieval scope violation: " + caseId);
                        measurement.put("evidenceSufficient",lastRetrieval.evidenceSufficient());
                        measurement.put("groundedConfidence",lastRetrieval.groundedConfidence());
                        measurement.put("diagnostic",lastRetrieval.diagnosticCode());
                        UUID expected = documents.get(row.path("documentSlug").asText());
                        measurement.put("hitAt5",lastRetrieval.results().stream().anyMatch(found -> found.documentId().equals(expected)));
                        measurement.put("evidence",lastRetrieval.results().stream().map(found -> {
                            var evidence = new LinkedHashMap<String,Object>();
                            evidence.put("documentId",found.documentId().toString()); evidence.put("chunkId",found.chunkId().toString());
                            evidence.put("title",found.title()); evidence.put("content",found.content());
                            evidence.put("accessScope", documentScopes.get(found.documentId())); return evidence;
                        }).toList());
                    }
                    results.add(measurement);
                    Path checkpoint = OUTPUT.resolve("checkpoints").resolve(caseId + ".json");
                    Files.createDirectories(checkpoint.getParent());
                    mapper.writerWithDefaultPrettyPrinter().writeValue(checkpoint.toFile(), measurement);
                    System.out.println("LIVE_CASE_DONE "+caseId+" ms="+measurement.get("totalMs")+" sufficient="+measurement.get("evidenceSufficient"));
                }
            }
        } finally {
            String outputName = Boolean.parseBoolean(System.getenv().getOrDefault("PHASE2_LIVE_RETRIEVAL_ONLY", "false"))
                    ? "live-retrieval-final.json" : !System.getenv().getOrDefault("PHASE2_LIVE_CASES", "").isBlank()
                    ? "live-rechecks.json" : Boolean.parseBoolean(System.getenv().getOrDefault("PHASE2_LIVE_ACCEPTANCE_ONLY","false"))
                    ? "live-acceptance.json" : "live-evaluation.json";
            mapper.writerWithDefaultPrettyPrinter().writeValue(OUTPUT.resolve(outputName).toFile(),results);
            var runtimeConfiguration = new LinkedHashMap<String,Object>(Map.of(
                    "capturedAt", Instant.now().toString(),
                    "requestTimeoutMs", assistantProperties.getRequestTimeout().toMillis(),
                    "generationTimeoutMs", assistantProperties.getGenerationTimeout().toMillis(),
                    "providerCallBudgetMs", geminiProperties.getCallBudget().toMillis(),
                    "generationMinIntervalMs", geminiProperties.getGenerationMinInterval().toMillis(),
                    "embeddingMinIntervalMs", geminiProperties.getEmbeddingMinInterval().toMillis(),
                    "workerRetryInitialMs", assistantProperties.getRetryInitialDelay().toMillis(),
                    "workerMaxAttempts", assistantProperties.getMaxAttempts(),
                    "timeoutWorkerRetryCapMs", 1000,
                    "answerCacheEnabled", false));
            runtimeConfiguration.put("modelRegistryMode", registrySnapshot.isBlank() ? "LEGACY_DECLARED_FIXTURE" : "READ_ONLY_APPROVED_REGISTRY_SNAPSHOT");
            runtimeConfiguration.put("modelRegistrySha256", System.getenv().getOrDefault("PHASE2_APPROVED_MODEL_SNAPSHOT_SHA256", ""));
            runtimeConfiguration.put("candidateModels", models.stream().map(model -> Map.of("modelId",model.getModelId(),
                    "status",model.getStatus().name(), "freeTierApproved",model.isFreeTierApproved(),
                    "maxOutputTokens",model.getMaxOutputTokens())).toList());
            mapper.writerWithDefaultPrettyPrinter().writeValue(OUTPUT.resolve("runtime-config.json").toFile(), runtimeConfiguration);
            mapper.writerWithDefaultPrettyPrinter().writeValue(OUTPUT.resolve("credential-runtime.json").toFile(),credentialPool.diagnostics());
            mapper.writerWithDefaultPrettyPrinter().writeValue(OUTPUT.resolve("credential-failures.json").toFile(),credentialPool.failureEvents());
        }
        assertFalse(results.isEmpty());
        for (var result : results) {
            if (!result.containsKey("jobStatus")) continue;
            assertEquals("COMPLETED", result.get("jobStatus"), result.get("caseId").toString());
            assertFalse(result.getOrDefault("answer", "").toString().isBlank(), result.get("caseId").toString());
            if (Boolean.TRUE.equals(result.get("personalProbe"))) {
                assertTrue(((List<?>) result.get("attempts")).isEmpty(), "Own lists must use zero model calls");
            }
        }
        long evaluated = results.stream().filter(row -> row.containsKey("hitAt5")).count();
        long hits = results.stream().filter(row -> Boolean.TRUE.equals(row.get("hitAt5"))).count();
        assertTrue(hits >= Math.ceil(evaluated * 0.95), "Recall@5 "+hits+"/"+evaluated+"; semantic review is separate");
    }

    @Test
    void directReadAuditPersistsWithoutInventingAModelRun() {
        var adapter = new com.ban.vehicle_management.infrastructure.persistence.adapter.ai.AiToolCallPersistenceAdapter(
                toolAuditRepository, toolAuditMapper);
        var audit = new com.ban.vehicle_management.domain.ai.model.AiToolCall();
        audit.setToolCallId(UUID.randomUUID());
        audit.setToolName("list_my_vehicles");
        audit.setToolType(com.ban.vehicle_management.shared.enumeration.ai.AiToolType.READ_ONLY);
        audit.setStatus(com.ban.vehicle_management.shared.enumeration.ai.AiToolCallStatus.EXECUTING);
        audit.setRequestPayloadRedacted("{}");
        audit.setArgumentPayloadRedacted("{}");
        audit.setResponsePayloadRedacted("{}");
        audit.setCreatedAt(Instant.now());
        var inserted = adapter.save(audit);
        org.junit.jupiter.api.Assertions.assertNull(inserted.getRunId());
        assertEquals(0, inserted.getVersion());
        inserted.setStatus(com.ban.vehicle_management.shared.enumeration.ai.AiToolCallStatus.SUCCEEDED);
        inserted.setResponsePayloadRedacted("{\"responseText\":\"Chưa có phương tiện.\"}");
        var updated = adapter.save(inserted);
        assertEquals(1, updated.getVersion());
        assertEquals(com.ban.vehicle_management.shared.enumeration.ai.AiToolCallStatus.SUCCEEDED,
                adapter.findById(updated.getToolCallId()).orElseThrow().getStatus());
    }

    private void runJob(JsonNode row, Map<String,Object> measurement) {
        UUID actor = UUID.randomUUID(); conversation = new ChatConversation(); conversation.setConversationId(UUID.randomUUID());
        input = new ChatMessage(); input.setMessageId(UUID.randomUUID()); input.setConversationId(conversation.getConversationId());
        input.setSenderAccountId(actor); input.setMessageType(ChatMessageType.TEXT);
        input.setContent(row.path("question").asText()); input.setCreatedAt(Instant.parse("2026-11-19T04:00:00Z"));
        List<ChatMessage> previous = new ArrayList<>();
        for (JsonNode value : row.path("context")) {
            ChatMessage message = new ChatMessage(); message.setMessageId(UUID.randomUUID());
            message.setConversationId(conversation.getConversationId()); message.setSenderAccountId(actor);
            message.setMessageType(ChatMessageType.TEXT); message.setContent(value.asText());
            message.setCreatedAt(input.getCreatedAt().minusSeconds(60)); previous.add(message);
        }
        history = new ArrayList<>(); history.add(input); history.addAll(previous);
        AssistantJob job = new AssistantJob(); job.setJobId(UUID.randomUUID()); job.setConversationId(conversation.getConversationId());
        job.setInputMessageId(input.getMessageId()); job.setAttemptCount(1);
        var retryEvents = new ArrayList<Map<String,Object>>();
        orchestrator.processJob(job);
        while (job.getStatus() == com.ban.vehicle_management.shared.enumeration.ai.AssistantJobStatus.RETRYING
                && Boolean.parseBoolean(System.getenv().getOrDefault("PHASE2_LIVE_DRIVE_RETRIES", "false"))
                && job.getAttemptCount() < 3) {
            long delay = Math.max(0L, java.time.Duration.between(Instant.now(), job.getNextAttemptAt()).toMillis());
            retryEvents.add(Map.of("attempt", job.getAttemptCount(), "failure", job.getErrorCode(), "waitMs", delay));
            try { Thread.sleep(delay); }
            catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException(exception); }
            // Emulate the worker claim only when due; preserve the same input/job and real backoff.
            job.setAttemptCount(job.getAttemptCount() + 1);
            job.setStatus(com.ban.vehicle_management.shared.enumeration.ai.AssistantJobStatus.PROCESSING);
            orchestrator.processJob(job);
        }
        measurement.put("retryEvents", retryEvents);
        measurement.put("attemptCount", job.getAttemptCount());
        measurement.put("jobStatus",String.valueOf(job.getStatus())); measurement.put("jobError",job.getErrorCode());
        measurement.put("answer",outputs.isEmpty() ? "" : outputs.getLast().getContent());
    }

    private EmbeddingVector liveDocumentVector(String text) throws Exception {
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(("gemini-embedding-2|768|"+text).getBytes(StandardCharsets.UTF_8)));
        Path file = VECTOR_CACHE.resolve("corpus-vectors").resolve(hash+".json");
        Path previous = Path.of("target/chatbox-phase2/corpus-vectors").resolve(hash+".json");
        if (!Files.exists(file) && Files.exists(previous)) file = previous;
        if (Files.exists(file)) return EmbeddingVector.of(mapper.readValue(file.toFile(),double[].class),768);
        var result = embeddings.embed(new EmbeddingRequest(text,null),embedding);
        assertTrue(result.isSuccess(), "Live corpus embedding unavailable: "+result.getFailure());
        Files.createDirectories(file.getParent()); mapper.writeValue(file.toFile(),result.getVector().values());
        return result.getVector();
    }

    private EmbeddingVector vector(int position) {
        double[] values = new double[768]; values[position] = 1; return EmbeddingVector.of(values,768);
    }
}
