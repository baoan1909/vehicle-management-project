package com.ban.vehicle_management.application.catalog.pricerule.port.out;

import com.ban.vehicle_management.domain.catalog.pricerule.model.PriceRule;
import com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PriceRulePortOut {
    PriceRule save(PriceRule priceRule);

    Optional<PriceRule> findById(UUID priceRuleId);

    List<PriceRule> findAll(
            UUID pricePlanId,
            UUID vehicleTypeId,
            UUID ticketTypeId,
            Boolean isActive,
            String keyword
    );

    boolean existsActiveVehicleTypeById(UUID vehicleTypeId);

    Optional<TicketType> findActiveTicketTypeById(UUID ticketTypeId);

    boolean existsActiveVisitorTimeOverlap(
            UUID pricePlanId,
            UUID vehicleTypeId,
            UUID ticketTypeId,
            LocalTime timeFrom,
            LocalTime timeTo,
            UUID excludedPriceRuleId
    );

    boolean existsActiveCustomerRule(
            UUID pricePlanId,
            UUID vehicleTypeId,
            UUID ticketTypeId,
            UUID excludedPriceRuleId
    );

    boolean hasUsage(UUID priceRuleId);

    Optional<PriceRule> findActiveSubscriptionRule(
            UUID vehicleTypeId,
            UUID ticketTypeId,
            LocalDate effectiveDate
    );
}