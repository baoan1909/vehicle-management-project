package com.ban.vehicle_management.domain.catalog.pricerule.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.catalog.pricerule.model.PriceRule;
import com.ban.vehicle_management.shared.enumeration.PriceRuleUnit;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PriceRulePolicyTest {

    private final PriceRulePolicy priceRulePolicy = new PriceRulePolicy();

    @Test
    void shouldInitializePriceRuleWithDefaults() {
        PriceRule priceRule = validPriceRule();
        priceRule.setUnit(null);
        priceRule.setLostCardFee(null);
        priceRule.setPriority(null);

        priceRulePolicy.initialize(priceRule);

        assertEquals(PriceRuleUnit.TURN, priceRule.getUnit());
        assertEquals(BigDecimal.ZERO, priceRule.getLostCardFee());
        assertEquals(0, priceRule.getPriority());
    }

    @Test
    void shouldRejectNegativeBasePrice() {
        PriceRule priceRule = validPriceRule();
        priceRule.setBasePrice(new BigDecimal("-1"));

        assertThrows(BadRequestException.class, () -> priceRulePolicy.initialize(priceRule));
    }

    @Test
    void shouldRejectHalfTimeWindow() {
        PriceRule priceRule = validPriceRule();
        priceRule.setTimeTo(null);

        assertThrows(BadRequestException.class, () -> priceRulePolicy.initialize(priceRule));
    }

    @Test
    void shouldRejectInvalidTimeRange() {
        PriceRule priceRule = validPriceRule();
        priceRule.setTimeFrom(LocalTime.of(18, 0));
        priceRule.setTimeTo(LocalTime.of(6, 0));

        assertThrows(BadRequestException.class, () -> priceRulePolicy.initialize(priceRule));
    }

    private PriceRule validPriceRule() {
        PriceRule priceRule = new PriceRule();
        priceRule.setPricePlanId(UUID.randomUUID());
        priceRule.setVehicleTypeId(UUID.randomUUID());
        priceRule.setRuleName("Gia ban ngay");
        priceRule.setTimeFrom(LocalTime.of(6, 0));
        priceRule.setTimeTo(LocalTime.of(18, 0));
        priceRule.setBasePrice(new BigDecimal("5000"));
        return priceRule;
    }
}

