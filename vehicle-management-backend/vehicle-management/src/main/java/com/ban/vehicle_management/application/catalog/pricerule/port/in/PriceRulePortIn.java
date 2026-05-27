package com.ban.vehicle_management.application.catalog.pricerule.port.in;

import com.ban.vehicle_management.domain.catalog.pricerule.model.PriceRule;
import java.util.List;
import java.util.UUID;

public interface PriceRulePortIn {
    PriceRule createPriceRule(PriceRule priceRule);

    PriceRule getPriceRuleById(UUID priceRuleId);

    List<PriceRule> getPriceRules(
            UUID pricePlanId,
            UUID vehicleTypeId,
            UUID ticketTypeId,
            Boolean isActive,
            String keyword
    );

    PriceRule updatePriceRule(UUID priceRuleId, PriceRule priceRule);

    void deletePriceRule(UUID priceRuleId);

    PriceRule activatePriceRule(UUID priceRuleId);
}