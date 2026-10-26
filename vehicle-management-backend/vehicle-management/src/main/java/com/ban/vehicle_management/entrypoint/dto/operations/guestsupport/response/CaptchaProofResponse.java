package com.ban.vehicle_management.entrypoint.dto.operations.guestsupport.response;

import java.time.Instant;

public record CaptchaProofResponse(String proof, Instant expiresAt) {
}
