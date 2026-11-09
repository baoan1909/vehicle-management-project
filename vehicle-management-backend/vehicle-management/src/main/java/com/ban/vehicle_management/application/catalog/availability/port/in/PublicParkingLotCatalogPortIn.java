package com.ban.vehicle_management.application.catalog.availability.port.in;

import com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType;
import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import com.ban.vehicle_management.domain.catalog.pricerule.model.PriceRule;
import com.ban.vehicle_management.domain.catalog.priceplan.model.PricePlan;
import com.ban.vehicle_management.shared.enumeration.catalog.PricePlanAppliesTo;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PublicParkingLotCatalogPortIn {
    List<PricePlan> getPricePlans(
            UUID parkingLotId,
            PricePlanAppliesTo appliesTo,
            LocalDate effectiveDate,
            String keyword
    );

    List<PriceRule> getPriceRules(
            UUID parkingLotId,
            UUID pricePlanId,
            UUID vehicleTypeId,
            UUID ticketTypeId,
            String keyword
    );

    List<VehicleType> getVehicleTypes(UUID parkingLotId);

    List<TicketType> getTicketTypes(UUID parkingLotId, String keyword);

    List<PriceRule> filterAvailablePriceRules(UUID parkingLotId, List<PriceRule> rules);
}
