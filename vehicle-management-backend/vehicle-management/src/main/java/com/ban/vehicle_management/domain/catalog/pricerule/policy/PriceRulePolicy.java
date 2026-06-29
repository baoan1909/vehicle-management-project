package com.ban.vehicle_management.domain.catalog.pricerule.policy;

import com.ban.vehicle_management.domain.catalog.pricerule.model.PriceRule;
import com.ban.vehicle_management.shared.enumeration.catalog.PriceRuleUnit;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.math.BigDecimal;

public class PriceRulePolicy {

    public void initialize(PriceRule priceRule) {
        requirePriceRule(priceRule);
        requireField(priceRule.getPricePlanId(), "pricePlanId");
        requireField(priceRule.getVehicleTypeId(), "vehicleTypeId");
        priceRule.setRuleName(TextValidationUtils.normalizeRequiredText(priceRule.getRuleName(), "ruleName", 150));
        requireField(priceRule.getBasePrice(), "basePrice");

        if (priceRule.getUnit() == null) {
            priceRule.setUnit(PriceRuleUnit.TURN);
        }
        if (priceRule.getLostCardFee() == null) {
            priceRule.setLostCardFee(BigDecimal.ZERO);
        }
        if (priceRule.getPriority() == null) {
            priceRule.setPriority(0);
        }

        if (priceRule.getBasePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("basePrice must not be negative");
        }
        if (priceRule.getLostCardFee().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("lostCardFee must not be negative");
        }
        if (priceRule.getPriority() < 0) {
            throw new BadRequestException("priority must not be negative");
        }

        boolean hasTimeFrom = priceRule.getTimeFrom() != null;
        boolean hasTimeTo = priceRule.getTimeTo() != null;
        if (hasTimeFrom != hasTimeTo) {
            throw new BadRequestException("timeFrom and timeTo must appear together");
        }
        if (hasTimeFrom && priceRule.getTimeFrom().equals(priceRule.getTimeTo())) {
            throw new BadRequestException("timeFrom must not equal timeTo");
        }

        if (priceRule.getIsActive() == null) {
            priceRule.setIsActive(Boolean.TRUE);
        }
    }

    private void requirePriceRule(PriceRule priceRule) {
        if (priceRule == null) {
            throw new BadRequestException("priceRule must not be null");
        }
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }

    public void activate(PriceRule priceRule) {
        requirePriceRule(priceRule);
        priceRule.setIsActive(Boolean.TRUE);
        initialize(priceRule);
    }

    public void deactivate(PriceRule priceRule) {
        requirePriceRule(priceRule);
        priceRule.setIsActive(Boolean.FALSE);
    }
}

