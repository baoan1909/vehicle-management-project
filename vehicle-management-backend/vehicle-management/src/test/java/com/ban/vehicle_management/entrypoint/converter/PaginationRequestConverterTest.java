package com.ban.vehicle_management.entrypoint.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PaginationRequestConverterTest {

    private final PaginationRequestConverter converter = new PaginationRequestConverter();

    @Test
    void resolveAppliesExistingApiDefaults() {
        var parameters = converter.resolve(null, null);

        assertEquals(0, parameters.page());
        assertEquals(10, parameters.size());
        assertEquals(0, converter.resolveLimit(null));
    }

    @Test
    void resolvePreservesProvidedValues() {
        var parameters = converter.resolve(3, 25);

        assertEquals(3, parameters.page());
        assertEquals(25, parameters.size());
        assertEquals(50, converter.resolveLimit(50));
    }
}
