package com.ban.vehicle_management.domain.ai.model;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable embedding vector. Rejects NaN/Infinite values and enforces the declared
 * dimension so corrupted provider output can never reach the vector store.
 */
public record EmbeddingVector(double[] values, int dimension) {

    public EmbeddingVector {
        Objects.requireNonNull(values, "Embedding values must not be null");
        if (dimension <= 0) {
            throw new IllegalArgumentException("Embedding dimension must be positive");
        }
        if (values.length != dimension) {
            throw new IllegalArgumentException("Embedding vector length must match declared dimension");
        }
        for (double value : values) {
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                throw new IllegalArgumentException("Embedding vector contains NaN or infinite values");
            }
        }
        values = Arrays.copyOf(values, values.length);
    }

    public static EmbeddingVector of(double[] values, int expectedDimension) {
        return new EmbeddingVector(values, expectedDimension);
    }

    @Override
    public double[] values() {
        return Arrays.copyOf(values, values.length);
    }

    @Override
    public int dimension() {
        return dimension;
    }
}