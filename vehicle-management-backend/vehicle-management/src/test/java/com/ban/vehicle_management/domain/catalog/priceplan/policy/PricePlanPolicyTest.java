package com.ban.vehicle_management.domain.catalog.priceplan.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.catalog.priceplan.model.PricePlan;
import com.ban.vehicle_management.shared.enumeration.catalog.PricePlanAppliesTo;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PricePlanPolicyTest {

    private final PricePlanPolicy pricePlanPolicy = new PricePlanPolicy();

    @Test
    void shouldInitializePricePlanWithDefaults() {
        PricePlan pricePlan = new PricePlan();
        pricePlan.setCode(" VISITOR-2026 ");
        pricePlan.setName(" Bang gia ");
        pricePlan.setAppliesTo(PricePlanAppliesTo.VISITOR);
        pricePlan.setEffectiveFrom(LocalDate.of(2026, 1, 1));

        pricePlanPolicy.initialize(pricePlan);

        assertEquals("VISITOR-2026", pricePlan.getCode());
        assertEquals("Bang gia", pricePlan.getName());
        assertEquals(Boolean.TRUE, pricePlan.getIsActive());
    }

    @Test
    void shouldRejectInvalidEffectiveRange() {
        PricePlan pricePlan = new PricePlan();
        pricePlan.setCode("MONTHLY-2026");
        pricePlan.setName("Bang gia thang");
        pricePlan.setAppliesTo(PricePlanAppliesTo.CUSTOMER);
        pricePlan.setEffectiveFrom(LocalDate.of(2026, 2, 1));
        pricePlan.setEffectiveTo(LocalDate.of(2026, 1, 1));

        assertThrows(BadRequestException.class, () -> pricePlanPolicy.initialize(pricePlan));
    }

    @Test
    void shouldDeactivatePricePlan() {
        PricePlan pricePlan = new PricePlan();
        pricePlan.setIsActive(Boolean.TRUE);

        pricePlanPolicy.deactivate(pricePlan);

        assertFalse(pricePlan.getIsActive());
    }

    @Test
    void shouldRejectPricePlanNameExceedingSchemaLength() {
        PricePlan pricePlan = new PricePlan();
        pricePlan.setCode("MONTHLY-2026");
        pricePlan.setName("A".repeat(151));
        pricePlan.setAppliesTo(PricePlanAppliesTo.CUSTOMER);
        pricePlan.setEffectiveFrom(LocalDate.of(2026, 1, 1));

        assertThrows(BadRequestException.class, () -> pricePlanPolicy.initialize(pricePlan));
    }
}

