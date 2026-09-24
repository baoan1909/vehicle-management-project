package com.ban.vehicle_management.application.ai.cache.model;

public record CachedEmbedding(int schemaVersion, String createdAt, double[] values, int dimension) {
}
