package com.ban.vehicle_management.ai.golden;

import static org.junit.jupiter.api.Assertions.*;

import com.ban.vehicle_management.application.ai.service.*;
import com.ban.vehicle_management.domain.ai.model.*;
import com.ban.vehicle_management.infrastructure.ai.extraction.MarkdownDocumentExtractor;
import com.ban.vehicle_management.infrastructure.ai.lmstudio.LmStudioProperties;
import com.ban.vehicle_management.infrastructure.persistence.adapter.ai.AiRuntimeModePersistenceAdapter;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeEmbeddingRepository;
import com.ban.vehicle_management.shared.enumeration.ai.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;

/** Test-only SQL fixture. The caller must use a disposable Testcontainers database. */
final class QwenApiCorpusFixture {
    static final Path AUTHORING = Path.of("docs/ai-knowledge/phase-0/authoring");
    private final JdbcTemplate jdbc;
    private final EmbeddingService embeddings;
    private final EmbeddingPromptFormatter formatter;
    private final LmStudioProperties local;
    private final ObjectMapper json;

    QwenApiCorpusFixture(JdbcTemplate jdbc, EmbeddingService embeddings,
            EmbeddingPromptFormatter formatter, LmStudioProperties local, ObjectMapper json) {
        this.jdbc = jdbc; this.embeddings = embeddings; this.formatter = formatter;
        this.local = local; this.json = json;
    }

    Map<String, Object> seed(Path output, AiRuntimeModePersistenceAdapter store) throws Exception {
        assertEquals("vehicle_management_test", jdbc.queryForObject("select current_database()", String.class));
        Files.createDirectories(output);
        // Seed migrations may contain demo sources; only the frozen16 sources are active here.
        jdbc.update("update ai.knowledge_sources set status='INACTIVE' where status='ACTIVE'");
        store.requestSwitch(AiRuntimeMode.QWEN_LOCAL, store.current().epoch(), UUID.randomUUID());
        store.advanceSwitch();
        assertEquals(AiRuntimeMode.QWEN_LOCAL, store.current().mode());
        UUID configuration = UUID.randomUUID(), index = UUID.randomUUID();
        jdbc.update("insert into ai.ai_model_configurations(configuration_id,provider,use_case,model_id,priority,rollout_percentage,status,output_dimension,free_tier_approved) values (?,'LM_STUDIO','EMBEDDING',?,1,100,'ACTIVE',768,true)", configuration, local.getEmbeddingModel());
        jdbc.update("insert into ai.knowledge_index_versions(index_version_id,version_code,model_configuration_id,provider,model_id,dimension,chunker_version,embedding_prompt_version,distance_metric,normalization,status,activated_at) values (?,?,?,'LM_STUDIO',?,768,?,?,'COSINE','L2','ACTIVE',now())", index, "api-" + index, configuration, local.getEmbeddingModel(), KnowledgeIndexVersionUseCaseImpl.CHUNKER_VERSION, EmbeddingPromptFormatter.QWEN_QA_V1);
        var model = new AiModelConfiguration(); model.setConfigurationId(configuration);
        model.setProvider(AiProvider.LM_STUDIO); model.setModelId(local.getEmbeddingModel());
        model.setUseCase(AiUseCase.EMBEDDING); model.setOutputDimension(768);
        model.setFreeTierApproved(true); model.setStatus(AiModelStatus.ACTIVE);
        var manifest = json.readTree(AUTHORING.resolve("knowledge-manifest.json").toFile());
        List<Map<String, Object>> documents = new ArrayList<>();
        List<Map<String, Object>> vectorEvidence = new ArrayList<>();
        int chunks = 0;
        for (var doc : manifest.path("documents")) {
            String slug = doc.path("slug").asText(), scope = doc.path("accessScope").asText();
            Path file = Path.of("docs/ai-knowledge/phase-0/knowledge").resolve(slug + ".md");
            String text = Files.readString(file);
            assertEquals(doc.path("exportSha256").asText(), hash(text), "Frozen export checksum differs");
            String title = Files.readString(AUTHORING.resolve("knowledge").resolve(slug + ".md"))
                    .lines().filter(line -> line.startsWith("title: ")).findFirst().orElseThrow().substring(7);
            UUID source = UUID.randomUUID(), document = UUID.randomUUID();
            jdbc.update("insert into ai.knowledge_sources(source_id,title,access_scope,status) values (?,?,?,'ACTIVE')", source, "ISOLATED API " + slug, scope);
            jdbc.update("insert into ai.knowledge_documents(document_id,source_id,document_key,title,document_version,access_scope,status,reviewed_at,reviewed_by,checksum_sha256) values (?,?,?,?,2,?,'READY',now(),?,?)", document, source, document, title, scope, UUID.randomUUID(), hash(text));
            var extracted = new MarkdownDocumentExtractor().extract(Files.readAllBytes(file), "md", "text/markdown");
            var chunked = new KnowledgeDocumentChunker().chunk(document, 2, title, extracted);
            for (var block : chunked.blocks()) {
                jdbc.update("insert into ai.knowledge_document_blocks(block_id,document_id,doc_version,block_index,kind,heading_path,source_page,source_section,content) values (?,?,2,?,?,?,?,?,?)", block.getBlockId(), document, block.getBlockIndex(), block.getKind().name(), block.getHeadingPath(), block.getSourcePage(), section(block.getSourceSection()), block.getContent());
            }
            for (var chunk : chunked.chunks()) {
                UUID chunkId = UUID.randomUUID();
                String input = formatter.formatDocument(EmbeddingPromptFormatter.QWEN_QA_V1, chunk.getTitle(), chunk.getContent());
                long started = System.nanoTime();
                var response = embeddings.embed(new EmbeddingRequest(input, null), model);
                Map<String, Object> evidence = new LinkedHashMap<>();
                evidence.put("slug", slug); evidence.put("chunkIndex", chunk.getChunkIndex());
                evidence.put("inputSha256", hash(input)); evidence.put("model", local.getEmbeddingModel());
                evidence.put("embeddingPrompt", EmbeddingPromptFormatter.QWEN_QA_V1);
                evidence.put("elapsedMs", (System.nanoTime() - started) / 1_000_000);
                evidence.put("success", response.isSuccess()); evidence.put("failure", response.getFailureCode());
                if (response.isSuccess()) evidence.put("realVector768", response.getVector().values());
                vectorEvidence.add(evidence);
                json.writerWithDefaultPrettyPrinter().writeValue(output.resolve("corpus-vectors.json").toFile(), vectorEvidence);
                assertTrue(response.isSuccess(), "Local fixture embedding failed; stop before API grading");
                jdbc.update("insert into ai.knowledge_chunks(chunk_id,document_id,title,content,chunk_index,access_scope,document_version,status,source_section,start_block_index,end_block_index) values (?,?,?,?,?,?,2,'READY',?,?,?)", chunkId, document, chunk.getTitle(), chunk.getContent(), chunk.getChunkIndex(), scope, section(chunk.getSourceSection()), chunk.getStartBlockIndex(), chunk.getEndBlockIndex());
                jdbc.update("insert into ai.knowledge_embeddings(embedding_id,chunk_id,embedding,embedding_model,embedding_dimension,document_version,index_version_id) values (?,?,cast(? as vector),?,768,2,?)", UUID.randomUUID(), chunkId, KnowledgeEmbeddingRepository.toVectorLiteral(response.getVector()), local.getEmbeddingModel(), index);
                chunks++;
            }
            documents.add(Map.of("slug", slug, "documentId", document, "sourceId", source,
                    "authoringVersion", doc.path("documentVersion").asText(), "persistedVersion", 2,
                    "scope", scope, "exportSha256", hash(text)));
            json.writerWithDefaultPrettyPrinter().writeValue(output.resolve("corpus-documents.json").toFile(), documents);
        }
        assertEquals(16, documents.size(), "Every shard must retain all16 corpus documents");
        jdbc.update("insert into ai.ai_model_configurations(configuration_id,provider,use_case,model_id,priority,rollout_percentage,status,max_output_tokens,free_tier_approved,requires_structured_output) values (?,'LM_STUDIO','SUPPORT_CHAT',?,1,100,'ACTIVE',512,true,true)", UUID.randomUUID(), local.getChatModel());
        store.advanceSwitch(); assertTrue(store.current().ready());
        return Map.of("documents", documents, "chunks", chunks, "indexId", index,
                "manifestSha256", hash(Files.readString(AUTHORING.resolve("knowledge-manifest.json"))),
                "simulatedApprovalOnly", true, "developerDatabaseModified", false);
    }

    static String hash(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    private String section(String value) { return value == null ? null : value.substring(0, Math.min(200, value.length())); }
}
