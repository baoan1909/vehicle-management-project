package com.ban.vehicle_management.infrastructure.persistence.database.repository.ai;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.ai.model.EmbeddingVector;
import org.junit.jupiter.api.Test;

class KnowledgeStagedEmbeddingRepositoryTest {

    @Test
    void shouldDecodePgVectorTextLiteral() {
        EmbeddingVector vector = KnowledgeStagedEmbeddingRepository.fromVectorLiteral(
                "[0.125,-2.5,3.75e-2]", 3);

        assertEquals(3, vector.dimension());
        assertArrayEquals(new double[]{0.125, -2.5, 0.0375}, vector.values(), 1.0e-12);
    }

    @Test
    void shouldRejectVectorWithUnexpectedDimension() {
        assertThrows(
                IllegalArgumentException.class,
                () -> KnowledgeStagedEmbeddingRepository.fromVectorLiteral("[0.1,0.2]", 3));
    }

    @Test
    void shouldRejectMalformedPgVectorLiteral() {
        assertThrows(
                IllegalArgumentException.class,
                () -> KnowledgeStagedEmbeddingRepository.fromVectorLiteral("0.1,0.2", 2));
    }
}
