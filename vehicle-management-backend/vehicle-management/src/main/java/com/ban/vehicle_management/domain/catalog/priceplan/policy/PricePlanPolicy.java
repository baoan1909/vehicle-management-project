package com.ban.vehicle_management.domain.catalog.priceplan.policy;

import com.ban.vehicle_management.domain.catalog.priceplan.model.PricePlan;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.enumeration.catalog.CatalogLifecycleStatus;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;

public class PricePlanPolicy {

    public void initialize(PricePlan pricePlan) {
        requirePricePlan(pricePlan);
        pricePlan.setCode(TextValidationUtils.normalizeCode(pricePlan.getCode(), "code", 50));
        pricePlan.setName(TextValidationUtils.normalizeRequiredText(pricePlan.getName(), "name", 150));
        pricePlan.setDescription(TextValidationUtils.normalizeNullableText(pricePlan.getDescription(), "description", 0));
        requireField(pricePlan.getAppliesTo(), "appliesTo");
        requireField(pricePlan.getEffectiveFrom(), "effectiveFrom");
        if (pricePlan.getEffectiveTo() != null && pricePlan.getEffectiveTo().isBefore(pricePlan.getEffectiveFrom())) {
            throw new BadRequestException("effectiveTo must not be before effectiveFrom");
        }
        if (pricePlan.getIsActive() == null) {
            pricePlan.setLifecycleStatus(CatalogLifecycleStatus.DRAFT);
        }
    }

    public void deactivate(PricePlan pricePlan) {
        requirePricePlan(pricePlan);
        pricePlan.setLifecycleStatus(CatalogLifecycleStatus.INACTIVE);
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

    public void activate(PricePlan pricePlan) {
        requirePricePlan(pricePlan);
        initialize(pricePlan);
        java.time.LocalDate today = com.ban.vehicle_management.shared.utils.DateTimeUtils.toVietnamLocalDate(java.time.Instant.now());
        if (pricePlan.getEffectiveTo() != null && pricePlan.getEffectiveTo().isBefore(today)) {
            throw new BadRequestException("Cannot activate an expired price plan; review effective dates first");
        }
        pricePlan.setLifecycleStatus(CatalogLifecycleStatus.ACTIVE);
    }
}

