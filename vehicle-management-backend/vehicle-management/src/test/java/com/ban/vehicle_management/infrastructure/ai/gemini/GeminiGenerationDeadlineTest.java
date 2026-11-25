package com.ban.vehicle_management.infrastructure.ai.gemini;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.ban.vehicle_management.application.ai.service.AiAssistantProperties;
import com.ban.vehicle_management.domain.ai.model.*;
import com.ban.vehicle_management.shared.enumeration.ai.AiUseCase;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GeminiGenerationDeadlineTest {
    @Spy GeminiProperties gemini = new GeminiProperties();
    @Spy AiAssistantProperties properties = new AiAssistantProperties();
    @Mock GeminiRequestMapper requests;
    @Mock GeminiResponseMapper responses;
    @InjectMocks GeminiAiProviderAdapter adapter;
    HttpServer server;

    @BeforeEach
    void server() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/", exchange -> {
            try {
                Thread.sleep(250);
                byte[] body = "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200,body.length); exchange.getResponseBody().write(body);
            } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            catch (java.io.IOException ignored) { /* Timed-out client closed the connection. */ }
            finally { exchange.close(); }
        });
        server.start();
        gemini.setApiKey("synthetic-test-key");
        gemini.setBaseUrl("http://127.0.0.1:"+server.getAddress().getPort());
        properties.setRequestTimeout(Duration.ofSeconds(2));
        properties.setGenerationTimeout(Duration.ofMillis(60));
        when(requests.toGenerateContentBody(any(),any())).thenReturn("{}");
    }

    @AfterEach void stop() { server.stop(0); }

    @Test
    void supportGenerationTimesOutEarlyAndRemainsEligibleForFallback() {
        AiModelConfiguration model = new AiModelConfiguration(); model.setUseCase(AiUseCase.SUPPORT_CHAT); model.setModelId("test-model");
        long start = System.nanoTime();
        AiResponse result = adapter.generate(new AiRequest("test",List.of(),false,null,List.of(),List.of(),List.of()),model);
        assertFalse(result.success()); assertEquals("TIMEOUT",result.failureCode()); assertTrue(result.retryable());
        assertTrue(Duration.ofNanos(System.nanoTime()-start).toMillis()<1500);
        verifyNoInteractions(responses);
    }

    @Test
    void separateDeadlineDoesNotShortenOtherUseCases() {
        AiModelConfiguration model = new AiModelConfiguration(); model.setUseCase(AiUseCase.EMBEDDING); model.setModelId("test-model");
        when(responses.toAiResponse(eq(200),anyString())).thenReturn(AiResponse.success("ok","{}",1,1));
        assertTrue(adapter.generate(new AiRequest("test",List.of(),false,null,List.of(),List.of(),List.of()),model).success());
    }
}
