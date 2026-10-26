package com.ban.vehicle_management.infrastructure.security.guestsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.operations.guestsupport.config.GuestSupportProperties;
import com.ban.vehicle_management.shared.exception.ServiceUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

class TurnstileCaptchaVerificationAdapterTest {

    @Test
    @SuppressWarnings("unchecked")
    void parsesSuccessfulProviderResponseWithoutExposingSecret() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("""
                {"success":true,"challenge_ts":"2026-10-26T04:00:00Z",
                 "hostname":"app.example.com","action":"support_intake"}
                """);
        when(client.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        TurnstileCaptchaVerificationAdapter adapter = new TurnstileCaptchaVerificationAdapter(
                properties(), new ObjectMapper(), client);

        var result = adapter.verify("opaque-provider-token", "127.0.0.1");

        assertEquals("support_intake", result.action());
        assertEquals("app.example.com", result.hostname());
    }

    @Test
    @SuppressWarnings("unchecked")
    void malformedResponseAndTimeoutAreUnavailableNotForbidden() throws Exception {
        HttpClient malformedClient = mock(HttpClient.class);
        HttpResponse<String> malformed = mock(HttpResponse.class);
        when(malformed.statusCode()).thenReturn(200);
        when(malformed.body()).thenReturn("{\"unexpected\":true}");
        when(malformedClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(malformed);
        assertThrows(ServiceUnavailableException.class, () -> new TurnstileCaptchaVerificationAdapter(
                properties(), new ObjectMapper(), malformedClient).verify("token", "127.0.0.1"));

        HttpClient timeoutClient = mock(HttpClient.class);
        when(timeoutClient.send(any(), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("timeout"));
        assertThrows(ServiceUnavailableException.class, () -> new TurnstileCaptchaVerificationAdapter(
                properties(), new ObjectMapper(), timeoutClient).verify("token", "127.0.0.1"));
    }

    private GuestSupportProperties properties() {
        GuestSupportProperties properties = new GuestSupportProperties();
        properties.setCaptchaProvider("TURNSTILE");
        properties.setCaptchaSecretKey("secret-not-logged");
        properties.setCaptchaVerifyUrl("https://challenges.cloudflare.com/turnstile/v0/siteverify");
        return properties;
    }
}
