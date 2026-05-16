package com.ban.vehicle_management.domain.catalog.pricerule.policy;

import com.ban.vehicle_management.domain.catalog.pricerule.model.PriceRule;
import com.ban.vehicle_management.shared.enumeration.PriceRuleUnit;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;

public class PriceRulePolicy {

    public void initialize(PriceRule priceRule) {
        requirePriceRule(priceRule);
        requireField(priceRule.getPricePlanId(), "pricePlanId");
        requireField(priceRule.getVehicleTypeId(), "vehicleTypeId");
        priceRule.setRuleName(normalizeRequired(priceRule.getRuleName(), "ruleName"));
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
        if (hasTimeFrom && !priceRule.getTimeFrom().isBefore(priceRule.getTimeTo())) {
            throw new BadRequestException("timeFrom must be before timeTo");
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

    private String normalizeRequired(String value, String fieldName) {
        String normalizedValue = normalizeNullable(value);
        if (normalizedValue == null) {
            throw new BadRequestException(fieldName + " must not be blank");
        }
        return normalizedValue;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }
}

