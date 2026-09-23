package com.ban.vehicle_management.domain.ai.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EmbeddingVectorTest {

    @Test
    void shouldCreateValidVector() {
        EmbeddingVector vector = EmbeddingVector.of(new double[]{1.0, 2.5, -3.0}, 3);
        assertArrayEquals(new double[]{1.0, 2.5, -3.0}, vector.values());
    }

    @Test
    void shouldRejectLengthMismatch() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new EmbeddingVector(new double[]{1.0, 2.0}, 3)
        );
    }

    @Test
    void shouldRejectNanValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new EmbeddingVector(new double[]{1.0, Double.NaN}, 2)
        );
    }

    @Test
    void shouldRejectInfiniteValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new EmbeddingVector(new double[]{1.0, Double.POSITIVE_INFINITY}, 2)
        );
    }

    @Test
    void shouldDefensivelyCopyValues() {
        double[] source = {1.0, 2.0};
        EmbeddingVector vector = EmbeddingVector.of(source, 2);
        source[0] = 99.0;
        assertArrayEquals(new double[]{1.0, 2.0}, vector.values());
    }
}