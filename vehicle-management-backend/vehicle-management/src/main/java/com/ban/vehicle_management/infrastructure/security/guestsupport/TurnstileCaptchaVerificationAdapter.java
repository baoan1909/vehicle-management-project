package com.ban.vehicle_management.infrastructure.security.guestsupport;

import com.ban.vehicle_management.application.operations.guestsupport.config.GuestSupportProperties;
import com.ban.vehicle_management.application.operations.guestsupport.model.CaptchaVerificationResult;
import com.ban.vehicle_management.application.operations.guestsupport.port.out.PublicChallengeVerificationPort;
import com.ban.vehicle_management.shared.exception.ServiceUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Cloudflare Turnstile server-side siteverify provider. */
@Component
public class TurnstileCaptchaVerificationAdapter implements PublicChallengeVerificationPort {

    private final GuestSupportProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public TurnstileCaptchaVerificationAdapter(GuestSupportProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    }

    TurnstileCaptchaVerificationAdapter(
            GuestSupportProperties properties,
            ObjectMapper objectMapper,
            HttpClient httpClient
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public CaptchaVerificationResult verify(String challengeToken, String remoteAddress) {
        if (!"TURNSTILE".equalsIgnoreCase(properties.getCaptchaProvider())) {
            throw new ServiceUnavailableException("CAPTCHA provider chưa được hỗ trợ");
        }
        String form = "secret=" + encode(properties.getCaptchaSecretKey())
                + "&response=" + encode(challengeToken)
                + (remoteAddress == null || remoteAddress.isBlank() ? "" : "&remoteip=" + encode(remoteAddress));
        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getCaptchaVerifyUrl()))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw unavailable();
            }
            return parse(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (IOException | IllegalArgumentException exception) {
            throw unavailable();
        }
    }

    private CaptchaVerificationResult parse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root == null || !root.has("success") || !root.get("success").isBoolean()) {
                throw unavailable();
            }
            boolean success = root.get("success").asBoolean();
            String action = text(root, "action");
            String hostname = text(root, "hostname");
            Instant challengedAt = parseInstant(text(root, "challenge_ts"));
            String reason = success ? null : firstError(root.get("error-codes"));
            return new CaptchaVerificationResult(success, action, hostname, challengedAt, reason);
        } catch (ServiceUnavailableException exception) {
            throw exception;
        } catch (RuntimeException | IOException exception) {
            throw unavailable();
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || !value.isTextual() ? null : value.asText();
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value);
        } catch (RuntimeException exception) {
            throw unavailable();
        }
    }

    private String firstError(JsonNode errors) {
        return errors != null && errors.isArray() && !errors.isEmpty() && errors.get(0).isTextual()
                ? errors.get(0).asText()
                : "provider_rejected";
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private ServiceUnavailableException unavailable() {
        return new ServiceUnavailableException("CAPTCHA provider đang tạm thời không khả dụng");
    }
}
