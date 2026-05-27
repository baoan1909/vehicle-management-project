package com.ban.vehicle_management.application.catalog.priceplan.port.out;

import com.ban.vehicle_management.domain.catalog.priceplan.model.PricePlan;
import com.ban.vehicle_management.shared.enumeration.catalog.PricePlanAppliesTo;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PricePlanPortOut {
    PricePlan save(PricePlan pricePlan);
    Optional<PricePlan> findById(UUID pricePlanId);
    List<PricePlan> findAll(Boolean isActive, PricePlanAppliesTo appliesTo, LocalDate effectiveDate, String keyword);
    boolean existsByCode(String code);
    boolean existsByCodeAndPricePlanIdNot(String code, UUID pricePlanId);
    boolean existsActiveOverlap(PricePlanAppliesTo appliesTo, LocalDate effectiveFrom, LocalDate effectiveTo, UUID excludedPricePlanId);
    boolean hasRules(UUID pricePlanId);
}