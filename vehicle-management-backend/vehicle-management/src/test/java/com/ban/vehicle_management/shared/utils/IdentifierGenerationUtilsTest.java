package com.ban.vehicle_management.shared.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdentifierGenerationUtilsTest {

    @Test
    void shouldGenerateDeterministicCustomerCodeWithPrefix() {
        UUID customerId = UUID.fromString("30000000-0000-0000-0000-000000000001");

        String customerCode = IdentifierGenerationUtils.generateCustomerCode(customerId);

        assertEquals("CUS-lR6UjegRxAM_M6DO14oXse3xwMcFonh4AHnz5BemNIQ", customerCode);
        assertTrue(customerCode.startsWith("CUS-"));
    }

    @Test
    void shouldRejectNullCustomerId() {
        assertThrows(IllegalArgumentException.class, () -> IdentifierGenerationUtils.generateCustomerCode(null));
    }
}
