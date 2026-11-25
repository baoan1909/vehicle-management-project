package com.ban.vehicle_management.ai.golden;

import static org.junit.jupiter.api.Assertions.*;
import com.ban.vehicle_management.application.ai.service.*;
import com.ban.vehicle_management.application.operations.guestsupport.service.GuestPublicAssistantService;
import com.ban.vehicle_management.domain.ai.model.*;
import com.ban.vehicle_management.infrastructure.ai.extraction.MarkdownDocumentExtractor;
import com.ban.vehicle_management.infrastructure.ai.lmstudio.LmStudioProviderAdapter;
import com.ban.vehicle_management.infrastructure.ai.lmstudio.LmStudioProperties;
import com.ban.vehicle_management.infrastructure.persistence.adapter.ai.AiRuntimeModePersistenceAdapter;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeEmbeddingRepository;
import com.ban.vehicle_management.shared.enumeration.ai.*;
import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Opt-in, real local models + real SQL retrieval; never imports into the developer database. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={"server.address=127.0.0.1", "app.ai.assistant-enabled=false", "app.ai.embedding.enabled=true",
        "app.ai.cache.enabled=false", "app.ai.ingestion.enabled=false",
        "app.ai.runtime-switch-initial-delay-ms=3600000", "app.ai.model-catalog-sync-initial-delay-ms=3600000",
        "app.ai.assistant-job-initial-delay-ms=3600000", "app.ai.embedding.build-initial-delay-ms=3600000"})
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named="AI_QWEN_LIVE",matches="true")
class QwenLocalAcceptanceIT extends com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport {
    private static final Path ROOT=Path.of("docs/ai-knowledge/phase-0/authoring");
    private static final Path OUT=Path.of(System.getenv().getOrDefault("AI_QWEN_LIVE_OUTPUT","target/qwen-live"));
    private static final String SCHEMA="""
        {"type":"object","properties":{"responseText":{"type":"string"},"citations":{"type":"array","items":{"type":"string"}}},"required":["responseText","citations"],"additionalProperties":false}
        """;
    @Autowired JdbcTemplate jdbc;
    @org.springframework.boot.test.web.server.LocalServerPort int localPort;
    @Autowired AiRuntimeModePersistenceAdapter store;
    @Autowired AiRuntimeModeService runtime;
    @Autowired LmStudioProviderAdapter local;
    @Autowired LmStudioProperties properties;
    @Autowired KnowledgeRetrievalService retrieval;
    @Autowired EmbeddingPromptFormatter formatter;
    @Autowired EmbeddingService embeddingService;
    @Autowired GuestPublicAssistantService guest;
    @Autowired AiAssistantProperties assistantSettings;
    private final ObjectMapper json=new ObjectMapper().findAndRegisterModules();
    private final Map<String,UUID> documents=new LinkedHashMap<>();
    private final List<Map<String,Object>> vectors=new ArrayList<>();
    private final List<Map<String,Object>> observations=new ArrayList<>();

    private AiModelConfiguration config(String id,AiUseCase useCase,UUID configuration) {
        var c=new AiModelConfiguration();c.setConfigurationId(configuration);c.setProvider(AiProvider.LM_STUDIO);
        c.setModelId(id);c.setUseCase(useCase);c.setOutputDimension(768);c.setMaxOutputTokens(512);
        c.setFreeTierApproved(true);c.setStatus(AiModelStatus.ACTIVE);return c;
    }
    private String hash(String input) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));
    }
    private EmbeddingVector realVector(String input,AiModelConfiguration configuration,String kind) throws Exception {
        long start=System.nanoTime();var response=embeddingService.embed(new EmbeddingRequest(input,null),configuration);
        var evidence=new LinkedHashMap<String,Object>();evidence.put("kind",kind);evidence.put("inputHash",hash(input));
        evidence.put("ms",(System.nanoTime()-start)/1_000_000);evidence.put("success",response.isSuccess());
        evidence.put("failure",response.getFailureCode());
        if(response.isSuccess()) evidence.put("vector",response.getVector().values());
        vectors.add(evidence);json.writerWithDefaultPrettyPrinter().writeValue(OUT.resolve("vectors.json").toFile(),vectors);
        assertTrue(response.isSuccess(),"Real local embedding failed: "+response.getFailureCode());return response.getVector();
    }
    @Test void actualLocalModelsRetrieveAndAnswerAgainstExistingCorpus() throws Exception {
        Files.createDirectories(OUT);
        var manifest=json.readTree(ROOT.resolve("knowledge-manifest.json").toFile());
        Map<String,Integer> batches=new HashMap<>();
        for(var doc:manifest.path("documents")) batches.put(doc.path("slug").asText(),doc.path("batch").asInt());
        List<JsonNode> rows=new ArrayList<>();
        for(var dataset:manifest.path("datasets")) {
            if(!"batch".equals(dataset.path("kind").asText()))continue;
            for(String line:Files.readAllLines(ROOT.resolve("evaluation").resolve(dataset.path("file").asText()))) {
                if(!line.isBlank()) rows.add(json.readTree(line));
            }
        }
        int batch=Integer.parseInt(System.getenv().getOrDefault("AI_QWEN_LIVE_BATCH","0"));
        int offset=Integer.parseInt(System.getenv().getOrDefault("AI_QWEN_LIVE_CASE_OFFSET","0"));
        int limit=Integer.parseInt(System.getenv().getOrDefault("AI_QWEN_LIVE_CASE_LIMIT","6"));
        var selection=QwenLiveCaseSelection.select(rows,batches,batch,offset,limit);
        var selected=selection.cases();
        store.requestSwitch(AiRuntimeMode.QWEN_LOCAL,store.current().epoch(),UUID.randomUUID());store.advanceSwitch();
        assertEquals(AiRuntimeMode.QWEN_LOCAL,store.current().mode());
        var modelIds=local.listModels().stream().map(AiProviderModel::modelId).toList();
        assertTrue(modelIds.containsAll(List.of(properties.getChatModel(),properties.getEmbeddingModel())),"Configured API model IDs unavailable");
        UUID embeddingId=UUID.randomUUID(),indexId=UUID.randomUUID();
        jdbc.update("insert into ai.ai_model_configurations(configuration_id,provider,use_case,model_id,priority,rollout_percentage,status,output_dimension,free_tier_approved) values (?,'LM_STUDIO','EMBEDDING',?,10,100,'ACTIVE',768,true)",embeddingId,properties.getEmbeddingModel());
        jdbc.update("insert into ai.knowledge_index_versions(index_version_id,version_code,model_configuration_id,provider,model_id,dimension,chunker_version,embedding_prompt_version,distance_metric,normalization,status,activated_at) values (?, ?, ?,'LM_STUDIO',?,768,?,?, 'COSINE','L2','ACTIVE',now())",indexId,"qwen-live-"+indexId,embeddingId,properties.getEmbeddingModel(),KnowledgeIndexVersionUseCaseImpl.CHUNKER_VERSION,EmbeddingPromptFormatter.QWEN_QA_V1);
        var embedding=config(properties.getEmbeddingModel(),AiUseCase.EMBEDDING,embeddingId);
        int chunks=0;
        for(var doc:manifest.path("documents")) {
            if(doc.path("batch").asInt()==0)continue;
            String slug=doc.path("slug").asText(),scope=doc.path("accessScope").asText();
            Path sourceFile=ROOT.resolve("knowledge").resolve(slug+".md");String text=Files.readString(sourceFile);
            Path file=Path.of("docs/ai-knowledge/phase-0/knowledge").resolve(slug+".md");
            assertEquals(doc.path("exportSha256").asText(),hash(Files.readString(file)),"Stale safe export");
            String title=text.lines().filter(l->l.startsWith("title: ")).findFirst().orElseThrow().substring(7);
            UUID source=UUID.randomUUID(),document=UUID.randomUUID();documents.put(slug,document);
            jdbc.update("insert into ai.knowledge_sources(source_id,title,access_scope,status) values (?,?,?,'ACTIVE')",source,"ISOLATED QWEN "+slug,scope);
            // Simulated approval in this disposable DB only, never authoring/dev publication.
            jdbc.update("insert into ai.knowledge_documents(document_id,source_id,document_key,title,document_version,access_scope,status,reviewed_at,reviewed_by,checksum_sha256) values (?,?,?,?,2,?,'READY',now(),?,?)",document,source,document,title,scope,UUID.randomUUID(),doc.path("exportSha256").asText());
            var extracted=new MarkdownDocumentExtractor().extract(Files.readAllBytes(file),"md","text/markdown");
            var chunked=new KnowledgeDocumentChunker().chunk(document,2,title,extracted);
            for(var block:chunked.blocks()) {
                jdbc.update("insert into ai.knowledge_document_blocks(block_id,document_id,doc_version,block_index,kind,heading_path,source_page,source_section,content) values (?,?,2,?,?,?,?,?,?)",block.getBlockId(),document,block.getBlockIndex(),block.getKind().name(),block.getHeadingPath(),block.getSourcePage(),section(block.getSourceSection()),block.getContent());
            }
            for(var chunk:chunked.chunks()) {
                UUID id=UUID.randomUUID();
                var vector=realVector(formatter.formatDocument(EmbeddingPromptFormatter.QWEN_QA_V1,chunk.getTitle(),chunk.getContent()),embedding,"DOCUMENT");
                jdbc.update("insert into ai.knowledge_chunks(chunk_id,document_id,title,content,chunk_index,access_scope,document_version,status,source_section,start_block_index,end_block_index) values (?,?,?,?,?,?,2,'READY',?,?,?)",id,document,chunk.getTitle(),chunk.getContent(),chunk.getChunkIndex(),scope,section(chunk.getSourceSection()),chunk.getStartBlockIndex(),chunk.getEndBlockIndex());
                jdbc.update("insert into ai.knowledge_embeddings(embedding_id,chunk_id,embedding,embedding_model,embedding_dimension,document_version,index_version_id) values (?,?,cast(? as vector),?,768,2,?)",UUID.randomUUID(),id,KnowledgeEmbeddingRepository.toVectorLiteral(vector),properties.getEmbeddingModel(),indexId);chunks++;
            }
        }
        store.advanceSwitch();assertTrue(store.current().ready());
        var chat=config(properties.getChatModel(),AiUseCase.SUPPORT_CHAT,UUID.randomUUID());
        var metadata=new LinkedHashMap<String,Object>();metadata.put("mode",Map.of("mode",store.current().mode().name(),"epoch",store.current().epoch(),"status",store.current().status(),"changedAt",store.current().changedAt().toString()));metadata.put("chatModel",properties.getChatModel());metadata.put("embeddingModel",properties.getEmbeddingModel());
        metadata.put("backendHttpPort",localPort);metadata.put("manifestHash",hash(Files.readString(ROOT.resolve("knowledge-manifest.json"))));metadata.put("harnessHash",hash(Files.readString(Path.of("src/test/java/com/ban/vehicle_management/ai/golden/QwenLocalAcceptanceIT.java"))));metadata.put("documents",documents.size());metadata.put("chunks",chunks);metadata.put("plannedCases",selected.size());metadata.put("contextTokens",properties.getContextTokens());metadata.put("maxInputBytes",properties.getMaxInputBytes());metadata.put("schemaHash",hash(SCHEMA));metadata.put("profile",LocalGroundedProfile.VERSION);metadata.put("fixtureNote","source_section display metadata capped at schema limit200; full content/title retained");
        json.writerWithDefaultPrettyPrinter().writeValue(OUT.resolve("metadata.json").toFile(),metadata);
        metadata.put("selection",Map.of("batch",batch,"offset",offset,"limit",limit,"totalEligible",selection.totalEligible(),"caseIds",selected.stream().map(row->row.path("caseId").asText()).toList()));
        metadata.put("fixtureApproval","Simulated in isolated Testcontainers only; source documents remain DRAFT");
        metadata.put("qualityAccepted",false);
        metadata.put("evaluationScope","Bounded static-query shard plus guest samples; not full984-case or authenticated end-to-end acceptance");
        json.writerWithDefaultPrettyPrinter().writeValue(OUT.resolve("metadata.json").toFile(),metadata);
        int infrastructureFailures=0;
        boolean abortedInfrastructure=false;
        for(var row:selected) {
            String question=row.path("question").asText(),slug=row.path("documentSlug").asText();
            var measurement=new LinkedHashMap<String,Object>();measurement.put("case",row);measurement.put("inputHash",hash(question));
            long started=System.nanoTime();
            long callsBefore=httpCount();
            try(var scope=runtime.openScope()) {
                var found=retrieval.search(KnowledgeRetrievalContext.global(),question,"PUBLIC".equals(row.path("persona").asText())?List.of("PUBLIC"):List.of("PUBLIC","CUSTOMER"),5);
                measurement.put("retrievalMs",(System.nanoTime()-started)/1_000_000);
                measurement.put("infrastructureFailure","EMBEDDING_FAILED".equals(found.diagnosticCode()));measurement.put("retrievalDiagnostic",found.diagnosticCode());measurement.put("retrieval",found.results());
                measurement.put("hit5",found.results().stream().anyMatch(r->documents.get(slug).equals(r.documentId())));
                measurement.put("retrievalMethod",found.retrievalMethod());measurement.put("sourceSnapshot",found.sourceSnapshot());
                var pack=found.isCommonFaq()?GroundedContextPack.canonical(found.results().getFirst()):LocalGroundedProfile.pack(question,found.results(),properties.getMaxInputBytes());measurement.put("evidence",pack);
                if(found.isCommonFaq()) {
                    boolean current=retrieval.verifyCommonEvidence(KnowledgeRetrievalContext.global(),"PUBLIC".equals(row.path("persona").asText())?List.of("PUBLIC"):List.of("PUBLIC","CUSTOMER"),found);
                    measurement.put("sourceStillEligible",current);measurement.put("generatedByModel",false);
                    if(current) {
                        measurement.put("answer",found.canonicalAnswer());
                        var validation=validate(found.canonicalAnswer(),List.of("C1"),pack,question);
                        measurement.put("outputValidatorPassed",validation.valid());measurement.put("outputViolations",validation.violations());
                        measurement.put("citations",List.of("C1"));measurement.put("citationLabelsValid",true);
                        if(!validation.valid()) measurement.put("failure","OUTPUT_VALIDATION_FAILED");
                    } else measurement.put("failure","COMMON_SOURCE_CHANGED");
                }
                else if(!found.evidenceSufficient() || pack.chunks().isEmpty()) {measurement.put("failure",found.diagnosticCode()!=null?found.diagnosticCode():"INSUFFICIENT_OR_UNPACKABLE_EVIDENCE");}
                else {
                    measurement.put("generatedByModel",true);
                    long generationStart=System.nanoTime();var answer=local.generate(LocalGroundedProfile.request(question,pack,SCHEMA),chat);
                    measurement.put("generationMs",(System.nanoTime()-generationStart)/1_000_000);measurement.put("inputTokens",answer.inputTokens());measurement.put("outputTokens",answer.outputTokens());measurement.put("providerSuccess",answer.success());measurement.put("failure",answer.failureCode());measurement.put("rawAnswer",answer.text());
                    if(Set.of("LOCAL_TIMEOUT","LOCAL_UNAVAILABLE","LOCAL_RESOURCE_BUSY").contains(String.valueOf(answer.failureCode()))) measurement.put("infrastructureFailure",true);
                    if(answer.success()) {
                        var envelope=json.readTree(answer.text());measurement.put("answer",envelope.path("responseText").asText());
                        var labels=new ArrayList<String>();for(var label:envelope.path("citations")) labels.add(label.asText());
                        var validation=validate(envelope.path("responseText").asText(),labels,pack,question);
                        measurement.put("outputValidatorPassed",validation.valid());measurement.put("outputViolations",validation.violations());
                        if(!validation.valid()) measurement.put("failure","OUTPUT_VALIDATION_FAILED");
                        var allowed=pack.chunks().stream().map(GroundedContextPack.PackChunk::label).toList();
                        measurement.put("citationLabelsValid",!labels.isEmpty()&&allowed.containsAll(labels));
                    }
                }
                runtime.assertCurrent();
            } catch(Exception failed) { measurement.put("exception",failed.getClass().getSimpleName()); if(failed instanceof org.springframework.dao.DataAccessException) measurement.put("infrastructureFailure",true); }
            measurement.put("totalMs",(System.nanoTime()-started)/1_000_000);measurement.put("modelHttpCalls",httpCount()-callsBefore);observations.add(measurement);
            json.writerWithDefaultPrettyPrinter().writeValue(OUT.resolve("observations.json").toFile(),observations);
            if("COMMON_FAQ".equals(measurement.get("retrievalMethod"))) assertEquals(0L,measurement.get("modelHttpCalls"),"Canonical FAQ must not call a model");
            infrastructureFailures=Boolean.TRUE.equals(measurement.get("infrastructureFailure"))?infrastructureFailures+1:0;
            if(infrastructureFailures>=2) {
                abortedInfrastructure=true;
                json.writerWithDefaultPrettyPrinter().writeValue(OUT.resolve("aborted.json").toFile(),Map.of("reason","Repeated retrieval infrastructure failures; stop calls instead of grading unavailable infrastructure", "completedCases",observations.size(),"plannedCases",selected.size()));
                break;
            }
        }
        assertFalse(abortedInfrastructure,"Live run stopped after repeated infrastructure failures; quality not accepted");
        jdbc.update("insert into ai.ai_model_configurations(configuration_id,provider,use_case,model_id,priority,rollout_percentage,status,max_output_tokens,free_tier_approved,requires_structured_output) values (?,'LM_STUDIO','SUPPORT_CHAT',?,10,100,'ACTIVE',512,true,true)",chat.getConfigurationId(),properties.getChatModel());
        assistantSettings.setAssistantEnabled(true);assistantSettings.setAllowUnpaidSupportData(true);
        List<Map<String,Object>> guestResults=new ArrayList<>();
        try {
            int sample=0;
            for(var row:selected) {
                if(!"PUBLIC".equals(row.path("persona").asText()) || !"ACCENTED".equals(row.path("variantType").asText()))continue;
                long start=System.nanoTime();var result=guest.answer(row.path("question").asText(),"qwen-local-"+sample,null,"192.0.2."+(sample+1));
                var entry=new LinkedHashMap<String,Object>();entry.put("case",row);entry.put("response",result);entry.put("totalMs",(System.nanoTime()-start)/1_000_000);guestResults.add(entry);
                if(++sample==4)break;
            }
        } finally { assistantSettings.setAssistantEnabled(false); }
        json.writerWithDefaultPrettyPrinter().writeValue(OUT.resolve("guest-runtime.json").toFile(),guestResults);
        var calls=jdbc.queryForList("SELECT provider,operation,http_status,count(*) AS calls FROM ai.provider_http_calls GROUP BY provider,operation,http_status ORDER BY provider,operation");
        json.writerWithDefaultPrettyPrinter().writeValue(OUT.resolve("http-calls.json").toFile(),calls);
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM ai.provider_http_calls WHERE provider='GEMINI'",Integer.class));
        assertFalse(observations.isEmpty());
        // Semantic thresholds are graded separately from protocol execution; this test never declares quality >=95%.
    }

    private String section(String value) { return value==null?null:value.substring(0,Math.min(200,value.length())); }

    private long httpCount() { return jdbc.queryForObject("select count(*) from ai.provider_http_calls",Long.class); }

    private AssistantOutputValidator.ValidationResult validate(String answer,List<String> labels,GroundedContextPack.ContextPack pack,String question) {
        Map<String,GroundedContextPack.PackChunk> allowed=new HashMap<>();for(var chunk:pack.chunks()) allowed.put(chunk.label(),chunk);
        if(!allowed.keySet().containsAll(labels)) return new AssistantOutputValidator.ValidationResult(false,List.of("CITATION_NOT_IN_ALLOWLIST"));
        var citations=labels.stream().map(label->{var chunk=allowed.get(label);return new AssistantResponseEnvelope.AssistantCitation(label,chunk.chunkId(),chunk.documentId(),chunk.title(),null,null);}).toList();
        var envelope=new AssistantResponseEnvelope(answer,citations,List.of(),1.0,false);
        return AssistantOutputValidator.validate(envelope,new AssistantOutputValidator.ValidationContext(allowed.keySet(),pack.chunks().stream().map(GroundedContextPack.PackChunk::chunkId).collect(java.util.stream.Collectors.toSet()),question,pack.chunks().stream().map(GroundedContextPack.PackChunk::content).toList(),List.of(),false,true));
    }

    @AfterEach void retainHttpAuditEvenWhenTheLiveShardFails() throws Exception {
        Files.createDirectories(OUT);
        var calls=jdbc.queryForList("SELECT provider,operation,http_status,count(*) AS calls FROM ai.provider_http_calls GROUP BY provider,operation,http_status ORDER BY provider,operation");
        json.writerWithDefaultPrettyPrinter().writeValue(OUT.resolve("http-calls.json").toFile(),calls);
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM ai.provider_http_calls WHERE provider='GEMINI'",Integer.class));
    }
}
