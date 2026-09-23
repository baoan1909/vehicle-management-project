package com.ban.vehicle_management.domain.ai.model;

public final class EmbeddingResult {

    private final boolean success;
    private final boolean retryable;
    private final EmbeddingVector vector;
    private final String modelId;
    private final EmbeddingFailure failure;

    private EmbeddingResult(
            boolean success,
            boolean retryable,
            EmbeddingVector vector,
            String modelId,
            EmbeddingFailure failure
    ) {
        this.success = success;
        this.retryable = retryable;
        this.vector = vector;
        this.modelId = modelId;
        this.failure = failure;
    }

    public static EmbeddingResult success(EmbeddingVector vector, String modelId) {
        return new EmbeddingResult(true, false, vector, modelId, null);
    }

    public static EmbeddingResult failure(EmbeddingFailure failure) {
        return new EmbeddingResult(false, failure.retryable(), null, null, failure);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public EmbeddingVector getVector() {
        return vector;
    }

    public String getModelId() {
        return modelId;
    }

    public String getFailureCode() {
        return failure == null ? null : failure.code();
    }

    public EmbeddingFailure getFailure() {
        return failure;
    }
}