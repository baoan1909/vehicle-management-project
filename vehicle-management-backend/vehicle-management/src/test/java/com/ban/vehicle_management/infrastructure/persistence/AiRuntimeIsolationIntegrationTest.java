package com.ban.vehicle_management.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;
import com.ban.vehicle_management.application.ai.service.*;
import com.ban.vehicle_management.domain.ai.model.*;
import com.ban.vehicle_management.infrastructure.ai.runtime.AiProviderHttpClient;
import com.ban.vehicle_management.infrastructure.ai.lmstudio.*;
import com.ban.vehicle_management.infrastructure.persistence.adapter.ai.AiRuntimeModePersistenceAdapter;
import com.ban.vehicle_management.shared.enumeration.ai.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import com.ban.vehicle_management.infrastructure.mapper.ai.AiRuntimeModePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.AiRuntimeModeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.AssistantJobRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeIndexVersionRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties="app.ai.runtime-switch-initial-delay-ms=3600000")
@ActiveProfiles("test")
class AiRuntimeIsolationIntegrationTest extends com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport {
    @Autowired JdbcTemplate jdbc;
    @Autowired AiRuntimeModeRepository runtimeRepository;
    @Autowired AssistantJobRepository jobRepository;
    @Autowired KnowledgeIndexVersionRepository indexRepository;
    @Autowired AiRuntimeModePersistenceMapper runtimeMapper;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired AiRuntimeModeService runtime;
    @Autowired AiRuntimeModePersistenceAdapter store;
    @Autowired AiProviderHttpClient dispatch;
    @Autowired com.ban.vehicle_management.application.ai.port.out.KnowledgeIndexVersionPortOut indexes;
    @Autowired org.springframework.transaction.support.TransactionTemplate tx;
    private volatile java.util.concurrent.CountDownLatch entered, releaseResponse;
    private HttpServer server;
    private final AtomicInteger calls=new AtomicInteger();
    private final ObjectMapper json=new ObjectMapper();
    private LmStudioProviderAdapter qwen;

    @BeforeEach void setup() throws Exception {
        jdbc.update("UPDATE ai.runtime_mode SET mode='GOOGLE_API',target_mode='GOOGLE_API',status='READY',epoch=epoch+1 WHERE singleton_id=1");
        entered=null; releaseResponse=null; calls.set(0);server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",exchange->{
            calls.incrementAndGet();
            if (entered != null) {
                entered.countDown();
                try { releaseResponse.await(5,java.util.concurrent.TimeUnit.SECONDS); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            }
            String path=exchange.getRequestURI().getPath();String body;
            if(path.endsWith("/embeddings")) {
                var node=json.createObjectNode();var vector=node.putArray("data").addObject().putArray("embedding");
                for(int i=0;i<1024;i++)vector.add(1.0);body=json.writeValueAsString(node);
            } else if(path.endsWith("/models")) body="{\"data\":[{\"id\":\"qwen3.5-4b\"},{\"id\":\"text-embedding-qwen3-embedding-0.6b\"},{\"id\":\"unapproved\"}]}";
            else body="{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"answer\"}}]}";
            byte[] bytes=body.getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });server.start();
        var props=new LmStudioProperties();props.setBaseUrl(base()+"/v1");
        qwen=new LmStudioProviderAdapter(props,new LmStudioWireMapper(json),json,dispatch);
    }
    private AiRuntimeModePersistenceAdapter newStore() {
        return new AiRuntimeModePersistenceAdapter(runtimeRepository, jobRepository, indexRepository,
                runtimeMapper, transactionManager);
    }
    private String base() { return "http://127.0.0.1:"+server.getAddress().getPort(); }
    @AfterEach void cleanup() {
        if(server!=null)server.stop(0);
        jdbc.update("UPDATE ai.runtime_mode SET mode='GOOGLE_API',target_mode='GOOGLE_API',status='READY',epoch=epoch+1 WHERE singleton_id=1");
    }
    private AiModelConfiguration config(String id) {
        var c=new AiModelConfiguration();c.setProvider(AiProvider.LM_STUDIO);c.setModelId(id);
        c.setMaxOutputTokens(512);c.setOutputDimension(768);return c;
    }
    private AiRequest question() { return new AiRequest("Answer safely",List.of(new AiRequestMessage("user","Hello")),false); }
    private HttpRequest request() { return HttpRequest.newBuilder(URI.create(base()+"/google")).GET().build(); }
    @Test void inactiveGenerationEmbeddingAndCatalogSendZeroRequestsInBothDirections() throws Exception {
        assertFalse(qwen.generate(question(),config("qwen3.5-4b")).success());
        assertFalse(qwen.embed(new EmbeddingRequest("query",null),config("text-embedding-qwen3-embedding-0.6b")).isSuccess());
        assertThrows(AiRuntimeModeChangedException.class,qwen::listModels);assertEquals(0,calls.get());
        dispatch.send(AiProvider.GEMINI,HttpClient.newHttpClient(),request());assertEquals(1,calls.get());
        store.requestSwitch(AiRuntimeMode.QWEN_LOCAL,store.current().epoch(),UUID.randomUUID());store.advanceSwitch();
        assertTrue(qwen.generate(question(),config("qwen3.5-4b")).success());
        assertTrue(qwen.embed(new EmbeddingRequest("query",null),config("text-embedding-qwen3-embedding-0.6b")).isSuccess());
        assertEquals(2,qwen.listModels().size());assertEquals(4,calls.get());
        assertThrows(AiRuntimeModeChangedException.class,()->dispatch.send(AiProvider.GEMINI,HttpClient.newHttpClient(),request()));
        assertEquals(4,calls.get());
        assertEquals(3,jdbc.queryForObject("SELECT count(*) FROM ai.provider_http_calls WHERE epoch=? AND provider='LM_STUDIO'",Integer.class,store.current().epoch()));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM ai.provider_http_calls WHERE epoch=? AND provider='GEMINI'",Integer.class,store.current().epoch()));
        assertEquals(AiRuntimeMode.QWEN_LOCAL,newStore().current().mode());
    }
    @Test void anotherInstanceCannotCompleteSwitchWhileOldDispatchLeaseExists() {
        long epoch=store.current().epoch();
        try(var lease=store.acquire(AiProvider.GEMINI,epoch);var scope=runtime.openScope()) {
            store.requestSwitch(AiRuntimeMode.QWEN_LOCAL,epoch,UUID.randomUUID());
            var other=newStore();other.advanceSwitch();
            assertTrue(store.current().switching());
            assertThrows(AiRuntimeModeChangedException.class,()->other.acquire(AiProvider.GEMINI,epoch));
            assertThrows(AiRuntimeModeChangedException.class,runtime::assertCurrent);
        }
        store.advanceSwitch();assertEquals(AiRuntimeMode.QWEN_LOCAL,store.current().mode());assertFalse(store.current().switching());
    }
    @Test void concurrentModeUpdatesRejectStaleEpoch() {
        long epoch=store.current().epoch();store.requestSwitch(AiRuntimeMode.QWEN_LOCAL,epoch,UUID.randomUUID());
        assertThrows(com.ban.vehicle_management.shared.exception.BadRequestException.class,
                ()->store.requestSwitch(AiRuntimeMode.GOOGLE_API,epoch,UUID.randomUUID()));
    }
    @Test void localConcurrencyLimitAppliesAcrossInstancesAndReleasesCleanly() {
        store.requestSwitch(AiRuntimeMode.QWEN_LOCAL,store.current().epoch(),UUID.randomUUID()); store.advanceSwitch();
        var other=newStore();
        try(var lease=store.acquire(AiProvider.LM_STUDIO,store.current().epoch())) {
            assertThrows(AiLocalBusyException.class,()->other.acquire(AiProvider.LM_STUDIO,store.current().epoch()));
        }
        try(var lease=other.acquire(AiProvider.LM_STUDIO,store.current().epoch())) { assertNotNull(lease); }
    }
    @Test void actualInflightHttpDrainsBeforeSwitchCanComplete() throws Exception {
        entered=new java.util.concurrent.CountDownLatch(1);releaseResponse=new java.util.concurrent.CountDownLatch(1);
        try(var executor=java.util.concurrent.Executors.newSingleThreadExecutor()) {
            var inFlight=executor.submit(()->dispatch.send(AiProvider.GEMINI,HttpClient.newHttpClient(),request()));
            assertTrue(entered.await(3,java.util.concurrent.TimeUnit.SECONDS));
            store.requestSwitch(AiRuntimeMode.QWEN_LOCAL,store.current().epoch(),UUID.randomUUID()); store.advanceSwitch();
            assertTrue(store.current().switching());
            releaseResponse.countDown();assertEquals(200,inFlight.get(5,java.util.concurrent.TimeUnit.SECONDS).statusCode());
            store.advanceSwitch();assertFalse(store.current().switching());
            assertThrows(AiRuntimeModeChangedException.class,()->dispatch.send(AiProvider.GEMINI,HttpClient.newHttpClient(),request()));
            assertEquals(1,calls.get());
        } finally { releaseResponse.countDown(); }
    }
    @Test void committedPublicationAndSwitchAcknowledgementAreSerialized() throws Exception {
        long epoch=store.current().epoch();
        try(var executor=java.util.concurrent.Executors.newSingleThreadExecutor()) {
            var attempt=new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.atomic.AtomicReference<java.util.concurrent.Future<AiRuntimeState>> future=new java.util.concurrent.atomic.AtomicReference<>();
            tx.executeWithoutResult(status->{
                store.lockPublication(epoch);
                future.set(executor.submit(()->{ attempt.countDown();return store.requestSwitch(AiRuntimeMode.QWEN_LOCAL,epoch,UUID.randomUUID()); }));
                try {
                    assertTrue(attempt.await(2,java.util.concurrent.TimeUnit.SECONDS));
                    assertThrows(java.util.concurrent.TimeoutException.class,()->future.get().get(150,java.util.concurrent.TimeUnit.MILLISECONDS));
                } catch(InterruptedException interrupted) { Thread.currentThread().interrupt();fail(interrupted); }
            });
            assertTrue(future.get().get(3,java.util.concurrent.TimeUnit.SECONDS).switching());
            assertThrows(AiRuntimeModeChangedException.class,()->tx.executeWithoutResult(status->store.lockPublication(epoch)));
        }
    }
    @Test void eachProviderKeepsItsOwnActiveIndexAndGoogleSnapshotSurvivesSwitch() {
        UUID googleConfig=UUID.randomUUID(),localConfig=UUID.randomUUID(),googleIndex=UUID.randomUUID(),localIndex=UUID.randomUUID();
        try {
            jdbc.update("insert into ai.ai_model_configurations(configuration_id,provider,use_case,model_id,priority,rollout_percentage,status,output_dimension) values (?,'GEMINI','EMBEDDING','google-existing',10,100,'ACTIVE',768)",googleConfig);
            jdbc.update("insert into ai.ai_model_configurations(configuration_id,provider,use_case,model_id,priority,rollout_percentage,status,output_dimension) values (?,'LM_STUDIO','EMBEDDING','qwen-existing',10,100,'ACTIVE',768)",localConfig);
            for(var pair:List.of(new Object[]{googleIndex,googleConfig,"GEMINI","google-existing","rag-qa-v1"},new Object[]{localIndex,localConfig,"LM_STUDIO","qwen-existing","qwen-qa-mrl768-v1"})) {
                jdbc.update("insert into ai.knowledge_index_versions(index_version_id,version_code,model_configuration_id,provider,model_id,dimension,chunker_version,embedding_prompt_version,distance_metric,normalization,status,activated_at) values (?,?,?,?,?,768,'test',?,'COSINE','L2','ACTIVE',now())",pair[0],"separate-"+pair[0],pair[1],pair[2],pair[3],pair[4]);
            }
            var googleSnapshot=jdbc.queryForMap("SELECT * FROM ai.knowledge_index_versions WHERE index_version_id=?",googleIndex);
            assertEquals(googleIndex,indexes.findActive().orElseThrow().getIndexVersionId());
            store.requestSwitch(AiRuntimeMode.QWEN_LOCAL,store.current().epoch(),UUID.randomUUID());store.advanceSwitch();
            assertTrue(store.current().ready());assertEquals(localIndex,indexes.findActive().orElseThrow().getIndexVersionId());
            assertEquals(googleSnapshot,jdbc.queryForMap("SELECT * FROM ai.knowledge_index_versions WHERE index_version_id=?",googleIndex));
            assertEquals(googleIndex,indexes.findActive(AiProvider.GEMINI).orElseThrow().getIndexVersionId());
        } finally {
            jdbc.update("DELETE FROM ai.knowledge_index_versions WHERE index_version_id IN (?,?)",googleIndex,localIndex);
            jdbc.update("DELETE FROM ai.ai_model_configurations WHERE configuration_id IN (?,?)",googleConfig,localConfig);
        }
    }
}
