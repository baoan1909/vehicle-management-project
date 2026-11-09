package com.ban.vehicle_management.application.catalog.availability.usecase;

import com.ban.vehicle_management.application.catalog.availability.port.in.PublicParkingLotCatalogPortIn;
import com.ban.vehicle_management.application.catalog.availability.port.out.ParkingLotCatalogAvailabilityPortOut;
import com.ban.vehicle_management.application.catalog.tickettype.port.out.TicketTypePortOut;
import com.ban.vehicle_management.application.catalog.vehicletype.port.out.VehicleTypePortOut;
import com.ban.vehicle_management.application.catalog.priceplan.port.out.PricePlanPortOut;
import com.ban.vehicle_management.application.catalog.pricerule.port.out.PriceRulePortOut;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType;
import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import com.ban.vehicle_management.domain.catalog.pricerule.model.PriceRule;
import com.ban.vehicle_management.domain.catalog.priceplan.model.PricePlan;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.enumeration.catalog.TicketTypeStatus;
import com.ban.vehicle_management.shared.enumeration.catalog.PricePlanAppliesTo;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicParkingLotCatalogUseCaseImpl implements PublicParkingLotCatalogPortIn {
    private final VehicleTypePortOut vehicleTypePortOut;
    private final TicketTypePortOut ticketTypePortOut;
    private final OrganizationPortOut organizationPortOut;
    private final ParkingLotPortOut parkingLotPortOut;
    private final ParkingLotCatalogAvailabilityPortOut availabilityPortOut;
    private final PricePlanPortOut pricePlanPortOut;
    private final PriceRulePortOut priceRulePortOut;

    public PublicParkingLotCatalogUseCaseImpl(VehicleTypePortOut vehicleTypePortOut,
                                              TicketTypePortOut ticketTypePortOut,
                                              OrganizationPortOut organizationPortOut,
                                              ParkingLotPortOut parkingLotPortOut,
                                              ParkingLotCatalogAvailabilityPortOut availabilityPortOut,
                                              PricePlanPortOut pricePlanPortOut,
                                              PriceRulePortOut priceRulePortOut) {
        this.vehicleTypePortOut = vehicleTypePortOut;
        this.ticketTypePortOut = ticketTypePortOut;
        this.organizationPortOut = organizationPortOut;
        this.parkingLotPortOut = parkingLotPortOut;
        this.availabilityPortOut = availabilityPortOut;
        this.pricePlanPortOut = pricePlanPortOut;
        this.priceRulePortOut = priceRulePortOut;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PricePlan> getPricePlans(
            UUID parkingLotId,
            PricePlanAppliesTo appliesTo,
            LocalDate effectiveDate,
            String keyword
    ) {
        return pricePlanPortOut.findAll(
                true,
                appliesTo,
                effectiveDate,
                keyword,
                Set.of(resolveOrganizationId(parkingLotId))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriceRule> getPriceRules(
            UUID parkingLotId,
            UUID pricePlanId,
            UUID vehicleTypeId,
            UUID ticketTypeId,
            String keyword
    ) {
        List<PriceRule> rules = priceRulePortOut.findAll(
                pricePlanId,
                vehicleTypeId,
                ticketTypeId,
                true,
                keyword,
                Set.of(resolveOrganizationId(parkingLotId))
        );
        return filterAvailablePriceRules(parkingLotId, rules);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleType> getVehicleTypes(UUID parkingLotId) {
        UUID organizationId = resolveOrganizationId(parkingLotId);
        Set<UUID> excluded = parkingLotId == null ? Set.of() : availabilityPortOut.findExcludedVehicleTypeIds(parkingLotId);
        return vehicleTypePortOut.findAll(true, Set.of(organizationId)).stream()
                .filter(type -> !excluded.contains(type.getVehicleTypeId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketType> getTicketTypes(UUID parkingLotId, String keyword) {
        UUID organizationId = resolveOrganizationId(parkingLotId);
        Set<UUID> excluded = parkingLotId == null ? Set.of() : availabilityPortOut.findExcludedTicketTypeIds(parkingLotId);
        return ticketTypePortOut.findAll(TicketTypeStatus.ACTIVE,
                        keyword == null || keyword.isBlank() ? null : keyword.trim(), Set.of(organizationId)).stream()
                .filter(type -> !excluded.contains(type.getTicketTypeId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriceRule> filterAvailablePriceRules(UUID parkingLotId, List<PriceRule> rules) {
        if (parkingLotId == null) return rules;
        resolveOrganizationId(parkingLotId);
        Set<UUID> excludedVehicles = availabilityPortOut.findExcludedVehicleTypeIds(parkingLotId);
        Set<UUID> excludedTickets = availabilityPortOut.findExcludedTicketTypeIds(parkingLotId);
        return rules.stream()
                .filter(rule -> !excludedVehicles.contains(rule.getVehicleTypeId()))
                .filter(rule -> !excludedTickets.contains(rule.getTicketTypeId()))
                .toList();
    }

    private UUID resolveOrganizationId(UUID parkingLotId) {
        if (parkingLotId == null) {
            return organizationPortOut.findByCode("COPARKING_INTERNAL")
                    .orElseThrow(() -> new NotFoundException("Default catalog not found"))
                    .getOrganizationId();
        }
        ParkingLot lot = parkingLotPortOut.findById(parkingLotId)
                .orElseThrow(() -> new NotFoundException("Parking lot not found"));
        if (lot.getStatus() == null || lot.getStatus() == ParkingLotStatus.CLOSED) {
            throw new NotFoundException("Public parking lot not found");
        }
        return lot.getOrganizationId();
    }
}
