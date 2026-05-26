package com.ban.vehicle_management.application.catalog.priceplan.port.in;

import com.ban.vehicle_management.domain.catalog.priceplan.model.PricePlan;
import com.ban.vehicle_management.shared.enumeration.catalog.PricePlanAppliesTo;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PricePlanPortIn {
    PricePlan createPricePlan(PricePlan pricePlan);
    PricePlan getPricePlanById(UUID pricePlanId);
    List<PricePlan> getPricePlans(Boolean isActive, PricePlanAppliesTo appliesTo, LocalDate effectiveDate, String keyword);
    PricePlan updatePricePlan(UUID pricePlanId, PricePlan pricePlan);
    void deletePricePlan(UUID pricePlanId);
    PricePlan activatePricePlan(UUID pricePlanId);
}