package com.ban.vehicle_management.domain.catalog.priceplan.policy;

import com.ban.vehicle_management.domain.catalog.priceplan.model.PricePlan;
import com.ban.vehicle_management.shared.exception.BadRequestException;

public class PricePlanPolicy {

    public void initialize(PricePlan pricePlan) {
        requirePricePlan(pricePlan);
        pricePlan.setCode(normalizeRequired(pricePlan.getCode(), "code"));
        pricePlan.setName(normalizeRequired(pricePlan.getName(), "name"));
        pricePlan.setDescription(normalizeNullable(pricePlan.getDescription()));
        requireField(pricePlan.getAppliesTo(), "appliesTo");
        requireField(pricePlan.getEffectiveFrom(), "effectiveFrom");
        if (pricePlan.getEffectiveTo() != null && pricePlan.getEffectiveTo().isBefore(pricePlan.getEffectiveFrom())) {
            throw new BadRequestException("effectiveTo must not be before effectiveFrom");
        }
        if (pricePlan.getIsActive() == null) {
            pricePlan.setIsActive(Boolean.TRUE);
        }
    }

    public void deactivate(PricePlan pricePlan) {
        requirePricePlan(pricePlan);
        pricePlan.setIsActive(Boolean.FALSE);
    }

    private void requirePricePlan(PricePlan pricePlan) {
        if (pricePlan == null) {
            throw new BadRequestException("pricePlan must not be null");
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

