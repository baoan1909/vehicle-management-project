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

    @Test
    void shouldGenerateStableParkingResourceCodesFromIdentifiers() {
        UUID identifier = UUID.fromString("31b6d833-211f-4669-8ba2-cdf89ba2420b");

        assertEquals("LOT-31B6D833211F", IdentifierGenerationUtils.generateParkingLotCode(identifier));
        assertEquals("ZONE-31B6D833211F", IdentifierGenerationUtils.generateZoneCode(identifier));
        assertEquals("GATE-31B6D833211F", IdentifierGenerationUtils.generateGateCode(identifier));
        assertEquals("LANE-31B6D833211F", IdentifierGenerationUtils.generateLaneCode(identifier));
    }

    @Test
    void shouldRejectNullParkingResourceIdentifiers() {
        assertThrows(IllegalArgumentException.class, () -> IdentifierGenerationUtils.generateParkingLotCode(null));
        assertThrows(IllegalArgumentException.class, () -> IdentifierGenerationUtils.generateZoneCode(null));
        assertThrows(IllegalArgumentException.class, () -> IdentifierGenerationUtils.generateGateCode(null));
        assertThrows(IllegalArgumentException.class, () -> IdentifierGenerationUtils.generateLaneCode(null));
    }
}
