package com.ban.vehicle_management.application.operations.guestsupport.model;

public record GuestRateLimitDecision(boolean allowed, long retryAfterSeconds) {
}
