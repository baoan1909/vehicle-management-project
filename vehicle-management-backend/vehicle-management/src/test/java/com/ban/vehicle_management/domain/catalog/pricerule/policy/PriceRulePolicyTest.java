package com.ban.vehicle_management.domain.catalog.pricerule.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.catalog.pricerule.model.PriceRule;
import com.ban.vehicle_management.shared.enumeration.catalog.PriceRuleUnit;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PriceRulePolicyTest {

    private final PriceRulePolicy priceRulePolicy = new PriceRulePolicy();

    @Test
    void shouldNormalizeFieldsAndSetDefaultsWhenInitialize() {
        PriceRule priceRule = validPriceRule();
        priceRule.setRuleName(" Gia ban ngay ");
        priceRule.setUnit(null);
        priceRule.setLostCardFee(null);
        priceRule.setPriority(null);
        priceRule.setIsActive(null);

        priceRulePolicy.initialize(priceRule);

        assertEquals("Gia ban ngay", priceRule.getRuleName());
        assertEquals(PriceRuleUnit.TURN, priceRule.getUnit());
        assertEquals(BigDecimal.ZERO, priceRule.getLostCardFee());
        assertEquals(0, priceRule.getPriority());
        assertEquals(Boolean.TRUE, priceRule.getIsActive());
    }

    @Test
    void shouldKeepExistingInactiveStatusWhenInitialize() {
        PriceRule priceRule = validPriceRule();
        priceRule.setIsActive(Boolean.FALSE);

        priceRulePolicy.initialize(priceRule);

        assertFalse(priceRule.getIsActive());
    }

    @Test
    void shouldRejectNullPriceRule() {
        assertThrows(BadRequestException.class, () -> priceRulePolicy.initialize(null));
    }

    @Test
    void shouldRejectMissingPricePlanId() {
        PriceRule priceRule = validPriceRule();
        priceRule.setPricePlanId(null);

        assertThrows(BadRequestException.class, () -> priceRulePolicy.initialize(priceRule));
    }

    @Test
    void shouldRejectMissingVehicleTypeId() {
        PriceRule priceRule = validPriceRule();
        priceRule.setVehicleTypeId(null);

        assertThrows(BadRequestException.class, () -> priceRulePolicy.initialize(priceRule));
    }

    @Test
    void shouldRejectBlankRuleName() {
        PriceRule priceRule = validPriceRule();
        priceRule.setRuleName(" ");

        assertThrows(BadRequestException.class, () -> priceRulePolicy.initialize(priceRule));
    }

    @Test
    void shouldRejectMissingBasePrice() {
        PriceRule priceRule = validPriceRule();
        priceRule.setBasePrice(null);

        assertThrows(BadRequestException.class, () -> priceRulePolicy.initialize(priceRule));
    }

    @Test
    void shouldRejectNegativeBasePrice() {
        PriceRule priceRule = validPriceRule();
        priceRule.setBasePrice(new BigDecimal("-1"));

        assertThrows(BadRequestException.class, () -> priceRulePolicy.initialize(priceRule));
    }

    @Test
    void shouldRejectNegativeLostCardFee() {
        PriceRule priceRule = validPriceRule();
        priceRule.setLostCardFee(new BigDecimal("-1"));

        assertThrows(BadRequestException.class, () -> priceRulePolicy.initialize(priceRule));
    }

    @Test
    void shouldRejectNegativePriority() {
        PriceRule priceRule = validPriceRule();
        priceRule.setPriority(-1);

        assertThrows(BadRequestException.class, () -> priceRulePolicy.initialize(priceRule));
    }

    @Test
    void shouldRejectMissingTimeToWhenTimeFromExists() {
        PriceRule priceRule = validPriceRule();
        priceRule.setTimeTo(null);

        assertThrows(BadRequestException.class, () -> priceRulePolicy.initialize(priceRule));
    }

    @Test
    void shouldRejectMissingTimeFromWhenTimeToExists() {
        PriceRule priceRule = validPriceRule();
        priceRule.setTimeFrom(null);

        assertThrows(BadRequestException.class, () -> priceRulePolicy.initialize(priceRule));
    }

    @Test
    void shouldRejectInvalidTimeRange() {
        PriceRule priceRule = validPriceRule();
        priceRule.setTimeFrom(LocalTime.of(18, 0));
        priceRule.setTimeTo(LocalTime.of(6, 0));

        assertThrows(BadRequestException.class, () -> priceRulePolicy.initialize(priceRule));
    }

    @Test
    void shouldRejectEqualTimeRange() {
        PriceRule priceRule = validPriceRule();
        priceRule.setTimeFrom(LocalTime.of(6, 0));
        priceRule.setTimeTo(LocalTime.of(6, 0));

        assertThrows(BadRequestException.class, () -> priceRulePolicy.initialize(priceRule));
    }

    @Test
    void shouldRejectRuleNameContainingUnsupportedCharacters() {
        PriceRule priceRule = validPriceRule();
        priceRule.setRuleName("Gia <ban ngay>");

        assertThrows(BadRequestException.class, () -> priceRulePolicy.initialize(priceRule));
    }

    @Test
    void shouldDeactivatePriceRule() {
        PriceRule priceRule = validPriceRule();
        priceRule.setIsActive(Boolean.TRUE);

        priceRulePolicy.deactivate(priceRule);

        assertFalse(priceRule.getIsActive());
    }

    @Test
    void shouldActivateAndValidatePriceRule() {
        PriceRule priceRule = validPriceRule();
        priceRule.setIsActive(Boolean.FALSE);

        priceRulePolicy.activate(priceRule);

        assertTrue(priceRule.getIsActive());
        assertEquals("Gia ban ngay", priceRule.getRuleName());
    }

    private PriceRule validPriceRule() {
        PriceRule priceRule = new PriceRule();
        priceRule.setPricePlanId(UUID.randomUUID());
        priceRule.setVehicleTypeId(UUID.randomUUID());
        priceRule.setRuleName("Gia ban ngay");
        priceRule.setTimeFrom(LocalTime.of(6, 0, 0));
        priceRule.setTimeTo(LocalTime.of(17, 59, 59));
        priceRule.setBasePrice(new BigDecimal("5000"));
        return priceRule;
    }
}

