package com.ban.vehicle_management.application.accesscontrol.subscription.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SubscriptionQrTokenServiceTest {

    private final SubscriptionQrTokenService tokenService = new SubscriptionQrTokenService();

    @Test
    void shouldRoundTripGeneratedTokenThroughQrPayload() {
        String token = tokenService.generateToken();
        String payload = tokenService.toPayload(token);

        assertTrue(payload.startsWith(SubscriptionQrTokenService.PAYLOAD_PREFIX));
        assertEquals(token, tokenService.extractToken(payload).orElseThrow());
    }

    @Test
    void shouldRejectMalformedQrPayloads() {
        assertTrue(tokenService.extractToken(null).isEmpty());
        assertTrue(tokenService.extractToken("COPARKING:SUBSCRIPTION:2:invalid").isEmpty());
        assertTrue(tokenService.extractToken(SubscriptionQrTokenService.PAYLOAD_PREFIX + "short").isEmpty());
    }
}
