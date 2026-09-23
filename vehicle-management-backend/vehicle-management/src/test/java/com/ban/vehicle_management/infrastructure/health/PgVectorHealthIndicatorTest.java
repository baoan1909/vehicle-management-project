package com.ban.vehicle_management.infrastructure.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PgVectorHealthIndicatorTest {

    @Test
    void shouldCompareSemanticVersionsNumerically() {
        assertTrue(PgVectorHealthIndicator.compareVersions("0.8.6", "0.8.0") > 0);
        assertTrue(PgVectorHealthIndicator.compareVersions("0.7.4", "0.8.0") < 0);
        assertEquals(0, PgVectorHealthIndicator.compareVersions("0.8.0", "0.8.0"));
    }
}
