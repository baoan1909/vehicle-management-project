package com.ban.vehicle_management.infrastructure.ai.runtime;

import com.ban.vehicle_management.application.ai.service.AiRuntimeModeService;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.stereotype.Component;

/** The shared DB dispatch lease fences every AI HTTP send across JVMs. */
@Component
public class AiProviderHttpClient {
    private final AiRuntimeModeService runtime;
    public AiProviderHttpClient(AiRuntimeModeService runtime) { this.runtime = runtime; }
    public HttpResponse<String> send(AiProvider provider, HttpClient client, HttpRequest request)
            throws IOException, InterruptedException {
        return send(provider, client, request, () -> { });
    }
    public HttpResponse<String> send(AiProvider provider, HttpClient client, HttpRequest request, Runnable beforeSend)
            throws IOException, InterruptedException {
        try (var lease = runtime.acquire(provider)) {
            var started = java.time.Instant.now();
            long tick = System.nanoTime();
            int status = 0;
            String failure = null;
            try {
                beforeSend.run();
                var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                status = response.statusCode();
                return response;
            } catch (IOException | InterruptedException exception) {
                failure = exception instanceof java.net.http.HttpTimeoutException ? "TIMEOUT" : "NETWORK_ERROR";
                throw exception;
            } finally {
                String path = request.uri().getPath();
                String operation = path.contains("embed") ? "EMBEDDING" : request.method().equals("GET") ? "CATALOG" : "GENERATION";
                runtime.recordHttpCall(lease.epoch(), provider, operation, started,
                        (System.nanoTime()-tick)/1_000_000, status, failure);
            }
        }
    }
}
