package com.ban.vehicle_management.domain.catalog.priceplan.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.catalog.priceplan.model.PricePlan;
import com.ban.vehicle_management.shared.enumeration.catalog.PricePlanAppliesTo;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PricePlanPolicyTest {

    private final PricePlanPolicy pricePlanPolicy = new PricePlanPolicy();

    @Test
    void shouldNormalizeFieldsAndSetActiveDefaultWhenInitialize() {
        PricePlan pricePlan = validPricePlan();
        pricePlan.setCode(" visitor-2027 ");
        pricePlan.setName(" Bang gia vang lai 2027 ");
        pricePlan.setDescription(" Gia ap dung cho khach vang lai ");
        pricePlan.setIsActive(null);

        pricePlanPolicy.initialize(pricePlan);

        assertEquals("VISITOR-2027", pricePlan.getCode());
        assertEquals("Bang gia vang lai 2027", pricePlan.getName());
        assertEquals("Gia ap dung cho khach vang lai", pricePlan.getDescription());
        assertEquals(Boolean.TRUE, pricePlan.getIsActive());
    }

    @Test
    void shouldKeepExistingInactiveStatusWhenInitialize() {
        PricePlan pricePlan = validPricePlan();
        pricePlan.setIsActive(Boolean.FALSE);

        pricePlanPolicy.initialize(pricePlan);

        assertFalse(pricePlan.getIsActive());
    }

    @Test
    void shouldRejectNullPricePlan() {
        assertThrows(BadRequestException.class, () -> pricePlanPolicy.initialize(null));
    }

    @Test
    void shouldRejectBlankCode() {
        PricePlan pricePlan = validPricePlan();
        pricePlan.setCode(" ");

        assertThrows(BadRequestException.class, () -> pricePlanPolicy.initialize(pricePlan));
    }

    @Test
    void shouldRejectBlankName() {
        PricePlan pricePlan = validPricePlan();
        pricePlan.setName(" ");

        assertThrows(BadRequestException.class, () -> pricePlanPolicy.initialize(pricePlan));
    }

    @Test
    void shouldRejectMissingAppliesTo() {
        PricePlan pricePlan = validPricePlan();
        pricePlan.setAppliesTo(null);

        assertThrows(BadRequestException.class, () -> pricePlanPolicy.initialize(pricePlan));
    }

    @Test
    void shouldRejectMissingEffectiveFrom() {
        PricePlan pricePlan = validPricePlan();
        pricePlan.setEffectiveFrom(null);

        assertThrows(BadRequestException.class, () -> pricePlanPolicy.initialize(pricePlan));
    }

    @Test
    void shouldRejectEffectiveToBeforeEffectiveFrom() {
        PricePlan pricePlan = validPricePlan();
        pricePlan.setEffectiveFrom(LocalDate.of(2027, 2, 1));
        pricePlan.setEffectiveTo(LocalDate.of(2027, 1, 31));

        assertThrows(BadRequestException.class, () -> pricePlanPolicy.initialize(pricePlan));
    }

    @Test
    void shouldRejectNameExceedingSchemaLength() {
        PricePlan pricePlan = validPricePlan();
        pricePlan.setName("A".repeat(151));

        assertThrows(BadRequestException.class, () -> pricePlanPolicy.initialize(pricePlan));
    }

    @Test
    void shouldDeactivatePricePlan() {
        PricePlan pricePlan = validPricePlan();
        pricePlan.setIsActive(Boolean.TRUE);

        pricePlanPolicy.deactivate(pricePlan);

        assertFalse(pricePlan.getIsActive());
    }

    @Test
    void shouldActivateAndValidatePricePlan() {
        PricePlan pricePlan = validPricePlan();
        pricePlan.setIsActive(Boolean.FALSE);

        pricePlanPolicy.activate(pricePlan);

        assertTrue(pricePlan.getIsActive());
        assertEquals("VISITOR-2027", pricePlan.getCode());
    }

    private PricePlan validPricePlan() {
        PricePlan pricePlan = new PricePlan();
        pricePlan.setCode("VISITOR-2027");
        pricePlan.setName("Bang gia vang lai 2027");
        pricePlan.setDescription("Gia ap dung cho khach vang lai");
        pricePlan.setAppliesTo(PricePlanAppliesTo.VISITOR);
        pricePlan.setEffectiveFrom(LocalDate.of(2027, 1, 1));
        pricePlan.setEffectiveTo(LocalDate.of(2027, 12, 31));
        return pricePlan;
    }
}

