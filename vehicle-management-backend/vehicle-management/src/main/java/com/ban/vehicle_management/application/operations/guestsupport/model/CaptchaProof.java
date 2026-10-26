package com.ban.vehicle_management.application.operations.guestsupport.model;

import java.time.Instant;

public record CaptchaProof(String token, Instant expiresAt) {
}
