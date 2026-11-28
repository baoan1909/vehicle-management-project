package com.ban.vehicle_management.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@Isolated("Upgrade a database containing both old providers without calling either provider")
class GoogleOnlyAiMigrationTest {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse(
            System.getenv().getOrDefault("POSTGRES_SPATIAL_TEST_IMAGE",
                    "vehicle-management/postgres-pgvector-postgis:17"))
            .asCompatibleSubstituteFor("postgres"));

    @Test
    void upgradesMixedDatabaseWithoutLosingGoogleKnowledgeOrReplayingLocalRequests() throws Exception {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .target("20261124170000").load().migrate();
        UUID localConfig = UUID.randomUUID(), googleConfig = UUID.randomUUID();
        UUID localIndex = UUID.randomUUID(), googleIndex = UUID.randomUUID();
        UUID source = UUID.randomUUID(), document = UUID.randomUUID(), chunk = UUID.randomUUID();
        UUID conversation = UUID.randomUUID(), input = UUID.randomUUID(), output = UUID.randomUUID();
        UUID localRun = UUID.randomUUID(), googleRun = UUID.randomUUID(), localJob = UUID.randomUUID();
        UUID tool = UUID.randomUUID();
        UUID pendingDocument = UUID.randomUUID(), pendingIngestion = UUID.randomUUID();
        String vector = "[" + "0,".repeat(767) + "1]";
        try (Connection db = connection(); var sql = db.createStatement()) {
            sql.executeUpdate("""
                    INSERT INTO ai.ai_model_configurations(configuration_id, provider, use_case, model_id, status, output_dimension)
                    VALUES ('%s','LM_STUDIO','EMBEDDING','retired-embedding','ACTIVE',768),
                           ('%s','GEMINI','EMBEDDING','gemini-embedding-001','ACTIVE',768)
                    """.formatted(localConfig, googleConfig));
            sql.executeUpdate("""
                    INSERT INTO ai.knowledge_index_versions(index_version_id, version_code, model_configuration_id,
                        provider, model_id, dimension, chunker_version, embedding_prompt_version,
                        distance_metric, normalization, status, activated_at)
                    VALUES ('%s','retired-index','%s','LM_STUDIO','retired-embedding',768,'chunker-v1','qwen-qa-mrl768-v1','COSINE','L2','ACTIVE',now()),
                           ('%s','google-index','%s','GEMINI','gemini-embedding-001',768,'chunker-v1','rag-qa-v1','COSINE','NONE','ACTIVE',now())
                    """.formatted(localIndex, localConfig, googleIndex, googleConfig));
            sql.executeUpdate("INSERT INTO ai.knowledge_sources(source_id,title,access_scope,status) VALUES ('%s','Keep source','PUBLIC','ACTIVE')".formatted(source));
            sql.executeUpdate("""
                    INSERT INTO ai.knowledge_documents(document_id,source_id,document_key,title,document_version,access_scope,status,reviewed_at,reviewed_by)
                    VALUES ('%s','%s','%s','Keep document',1,'PUBLIC','READY',now(),'%s')
                    """.formatted(document, source, UUID.randomUUID(), UUID.randomUUID()));
            sql.executeUpdate("""
                    INSERT INTO ai.knowledge_chunks(chunk_id,document_id,title,content,chunk_index,access_scope,document_version,status)
                    VALUES ('%s','%s','Keep FAQ','How? Keep reviewed answer.',0,'PUBLIC',1,'READY')
                    """.formatted(chunk, document));
            sql.executeUpdate("""
                    INSERT INTO ai.knowledge_embeddings(chunk_id,embedding,embedding_model,embedding_dimension,document_version,index_version_id)
                    VALUES ('%s','%s'::vector,'retired-embedding',768,1,'%s'),
                           ('%s','%s'::vector,'gemini-embedding-001',768,1,'%s')
                    """.formatted(chunk, vector, localIndex, chunk, vector, googleIndex));
            sql.executeUpdate("""
                    INSERT INTO ai.knowledge_staged_embeddings(chunk_id,provider,model_configuration_id,model_id,
                        embedding_dimension,embedding_prompt_version,chunker_version,content_hash,embedding_fingerprint,embedding)
                    VALUES ('%s','LM_STUDIO','%s','retired-embedding',768,'qwen-qa-mrl768-v1','chunker-v1','%s','%s','%s'::vector)
                    """.formatted(chunk, localConfig, "a".repeat(64), "b".repeat(64), vector));
            sql.executeUpdate("INSERT INTO operations.chat_conversations(conversation_id,conversation_type,status) VALUES ('%s','ASSISTANT_SUPPORT','ACTIVE')".formatted(conversation));
            sql.executeUpdate("""
                    INSERT INTO operations.chat_messages(message_id,conversation_id,message_type,content)
                    VALUES ('%s','%s','TEXT','Keep question'),('%s','%s','ASSISTANT_TEXT','Keep answer')
                    """.formatted(input, conversation, output, conversation));
            sql.executeUpdate("""
                    INSERT INTO ai.ai_runs(run_id,conversation_id,input_message_id,provider,model_id,prompt_version,status)
                    VALUES ('%s','%s','%s','LM_STUDIO','retired-chat','test','FAILED'),
                           ('%s','%s','%s','GEMINI','gemini-chat','test','FAILED')
                    """.formatted(localRun, conversation, input, googleRun, conversation, input));
            sql.executeUpdate("INSERT INTO ai.ai_tool_calls(tool_call_id,run_id,tool_name,status) VALUES ('%s','%s','test_pending_action','REQUESTED')".formatted(tool, localRun));
            sql.executeUpdate("UPDATE ai.runtime_mode SET mode='QWEN_LOCAL', target_mode='QWEN_LOCAL', status='READY', epoch=epoch+1");
            sql.executeUpdate("INSERT INTO ai.assistant_jobs(job_id,input_message_id,conversation_id,status) VALUES ('%s','%s','%s','PENDING')".formatted(localJob, input, conversation));
            sql.executeUpdate("INSERT INTO ai.provider_http_calls(call_id,epoch,provider,operation,started_at,latency_ms,http_status) VALUES (gen_random_uuid(),2,'LM_STUDIO','GENERATION',now(),1,200)");
            sql.executeUpdate("""
                    INSERT INTO ai.knowledge_documents(document_id,source_id,document_key,title,document_version,access_scope,status)
                    VALUES ('%s','%s','%s','Do not upload silently',1,'PUBLIC','PENDING')
                    """.formatted(pendingDocument, source, UUID.randomUUID()));
            sql.executeUpdate("INSERT INTO ai.knowledge_ingestion_jobs(ingestion_job_id,document_id,status) VALUES ('%s','%s','PENDING')".formatted(pendingIngestion, pendingDocument));

            String googleSnapshot = text(db, "SELECT to_jsonb(t)::text FROM ai.knowledge_index_versions t WHERE index_version_id='%s'".formatted(googleIndex));
            Flyway flyway = Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()).load();
            flyway.migrate();
            assertEquals(0, flyway.info().pending().length);
            flyway.validate();
            assertEquals(googleSnapshot, text(db, "SELECT to_jsonb(t)::text FROM ai.knowledge_index_versions t WHERE index_version_id='%s'".formatted(googleIndex)));
            assertEquals("1", text(db, "SELECT count(*) FROM ai.ai_model_configurations WHERE configuration_id='%s'".formatted(googleConfig)));
            assertEquals("1", text(db, "SELECT count(*) FROM ai.knowledge_embeddings WHERE index_version_id='%s'".formatted(googleIndex)));
            assertEquals("1", text(db, "SELECT count(*) FROM ai.ai_runs WHERE run_id='%s'".formatted(googleRun)));
            assertEquals("1", text(db, "SELECT count(*) FROM ai.knowledge_documents WHERE document_id='%s' AND status='READY'".formatted(document)));
            assertEquals("2", text(db, "SELECT count(*) FROM operations.chat_messages WHERE conversation_id='%s'".formatted(conversation)));
            assertEquals("FAILED:AI_PROVIDER_REMOVED", text(db, "SELECT status || ':' || error_code FROM ai.assistant_jobs WHERE job_id='%s'".formatted(localJob)));
            assertEquals("FAILED:AI_PROVIDER_REMOVED", text(db, "SELECT status || ':' || error_code FROM ai.knowledge_ingestion_jobs WHERE ingestion_job_id='%s'".formatted(pendingIngestion)));
            assertEquals("FAILED:AI_PROVIDER_REMOVED", text(db, "SELECT status || ':' || failure_code FROM ai.knowledge_documents WHERE document_id='%s'".formatted(pendingDocument)));
            assertEquals("EXPIRED:AI_PROVIDER_REMOVED:true", text(db, "SELECT status || ':' || failure_code || ':' || (run_id IS NULL)::text FROM ai.ai_tool_calls WHERE tool_call_id='%s'".formatted(tool)));
            assertEquals("1", text(db, "SELECT count(*) FROM ai.retired_provider_records WHERE record_table='ai_runs' AND record_key='%s' AND payload->>'provider'='LM_STUDIO'".formatted(localRun)));
            assertEquals("1", text(db, "SELECT count(*) FROM ai.retired_provider_records WHERE record_table='assistant_jobs' AND record_key='%s' AND payload->>'status'='PENDING'".formatted(localJob)));
            assertEquals("0", text(db, "SELECT count(*) FROM ai.knowledge_staged_embeddings WHERE provider='LM_STUDIO'"));
            assertEquals("0", text(db, "SELECT count(*) FROM ai.knowledge_index_versions WHERE provider='LM_STUDIO'"));
            assertEquals("0", text(db, "SELECT count(*) FROM information_schema.columns WHERE table_schema='ai' AND table_name='assistant_jobs' AND column_name IN ('runtime_mode','runtime_epoch')"));
            assertEquals("0", text(db, "SELECT count(*) FROM information_schema.tables WHERE table_schema='ai' AND table_name IN ('runtime_mode','runtime_mode_events','provider_http_calls')"));
            SQLException rejected = assertThrows(SQLException.class, () -> sql.executeUpdate("INSERT INTO ai.ai_model_catalog(provider,model_id) VALUES ('LM_STUDIO','must-not-return')"));
            assertEquals("23514", rejected.getSQLState());

            String archiveQuery = "SELECT jsonb_agg(to_jsonb(t) ORDER BY record_table, record_key)::text FROM ai.retired_provider_records t";
            String archived = text(db, archiveQuery);
            db.setAutoCommit(false);
            try {
                sql.execute(Files.readString(Path.of(
                        "src/main/resources/db/migration/V20261126120000__retire_local_ai_provider.sql")));
                db.commit();
            } finally {
                db.rollback();
                db.setAutoCommit(true);
            }
            assertEquals(archived, text(db, archiveQuery));
            assertEquals(googleSnapshot, text(db, "SELECT to_jsonb(t)::text FROM ai.knowledge_index_versions t WHERE index_version_id='%s'".formatted(googleIndex)));
            assertEquals("FAILED:AI_PROVIDER_REMOVED", text(db, "SELECT status || ':' || error_code FROM ai.assistant_jobs WHERE job_id='%s'".formatted(localJob)));
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static String text(Connection db, String query) throws SQLException {
        try (var statement = db.createStatement(); var rows = statement.executeQuery(query)) {
            assertTrue(rows.next());
            return rows.getString(1);
        }
    }
}
