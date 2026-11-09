package com.ban.vehicle_management.application.parking.parkinglot.usecase;

import com.ban.vehicle_management.application.catalog.availability.port.in.PublicParkingLotCatalogPortIn;
import com.ban.vehicle_management.application.catalog.priceplan.port.out.PricePlanPortOut;
import com.ban.vehicle_management.application.catalog.pricerule.port.out.PriceRulePortOut;
import com.ban.vehicle_management.application.parking.availability.ParkingAvailabilityQuery;
import com.ban.vehicle_management.application.parking.parkinglot.model.result.PublicParkingLotDirectoryResult;
import com.ban.vehicle_management.application.parking.parkinglot.port.in.PublicParkingLotDirectoryPortIn;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.parking.parkinglotreview.model.ReviewAggregate;
import com.ban.vehicle_management.application.parking.parkinglotreview.port.out.ParkingLotReviewPortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.domain.catalog.priceplan.model.PricePlan;
import com.ban.vehicle_management.domain.catalog.pricerule.model.PriceRule;
import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import com.ban.vehicle_management.domain.parking.availability.model.ParkingAvailabilitySnapshot;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicParkingLotDirectoryUseCaseImpl implements PublicParkingLotDirectoryPortIn {

    private final ParkingLotPortOut parkingLotPortOut;
    private final ZonePortOut zonePortOut;
    private final PublicParkingLotCatalogPortIn publicParkingLotCatalogPortIn;
    private final ParkingAvailabilityQuery parkingAvailabilityQuery;
    private final PricePlanPortOut pricePlanPortOut;
    private final PriceRulePortOut priceRulePortOut;
    private final ParkingSessionPortOut parkingSessionPortOut;
    private final ParkingLotReviewPortOut parkingLotReviewPortOut;

    public PublicParkingLotDirectoryUseCaseImpl(
            ParkingLotPortOut parkingLotPortOut,
            ZonePortOut zonePortOut,
            PublicParkingLotCatalogPortIn publicParkingLotCatalogPortIn,
            ParkingAvailabilityQuery parkingAvailabilityQuery,
            PricePlanPortOut pricePlanPortOut,
            PriceRulePortOut priceRulePortOut,
            ParkingSessionPortOut parkingSessionPortOut,
            ParkingLotReviewPortOut parkingLotReviewPortOut
    ) {
        this.parkingLotPortOut = parkingLotPortOut;
        this.zonePortOut = zonePortOut;
        this.publicParkingLotCatalogPortIn = publicParkingLotCatalogPortIn;
        this.parkingAvailabilityQuery = parkingAvailabilityQuery;
        this.pricePlanPortOut = pricePlanPortOut;
        this.priceRulePortOut = priceRulePortOut;
        this.parkingSessionPortOut = parkingSessionPortOut;
        this.parkingLotReviewPortOut = parkingLotReviewPortOut;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingLot> getActiveParkingLots() {
        return parkingLotPortOut.findAll(ParkingLotStatus.ACTIVE, null, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicParkingLotDirectoryResult> getDirectory() {
        Instant snapshotAt = Instant.now();
        LocalDate businessDate = DateTimeUtils.toAppLocalDate(snapshotAt);
        List<ParkingLot> lots = parkingLotPortOut.findAll(ParkingLotStatus.ACTIVE, null, null, null);
        Map<UUID, ReviewAggregate> reviewAggregates = parkingLotReviewPortOut.getAggregates(
                lots.stream().map(ParkingLot::getParkingLotId).collect(java.util.stream.Collectors.toSet()));
        return lots.stream()
                .sorted(Comparator.comparing(ParkingLot::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(lot -> toResult(lot, businessDate, snapshotAt,
                        reviewAggregates.getOrDefault(lot.getParkingLotId(), ReviewAggregate.empty())))
                .toList();
    }

    private PublicParkingLotDirectoryResult toResult(
            ParkingLot lot,
            LocalDate businessDate,
            Instant snapshotAt,
            ReviewAggregate reviewAggregate
    ) {
        List<Zone> zones = zonePortOut.findAll(lot.getParkingLotId(), null, ZoneStatus.ACTIVE, null);
        Map<UUID, AvailabilityCounters> countersByVehicleType = new LinkedHashMap<>();
        for (Zone zone : zones) {
            if (zone.getVehicleTypeId() == null) {
                continue;
            }
            ParkingAvailabilitySnapshot snapshot = parkingAvailabilityQuery.get(zone, businessDate);
            countersByVehicleType
                    .computeIfAbsent(zone.getVehicleTypeId(), ignored -> new AvailabilityCounters())
                    .add(snapshot);
        }

        Map<UUID, VehicleType> availableTypes = new LinkedHashMap<>();
        for (VehicleType vehicleType : publicParkingLotCatalogPortIn.getVehicleTypes(lot.getParkingLotId())) {
            availableTypes.put(vehicleType.getVehicleTypeId(), vehicleType);
        }

        boolean operational = lot.getStatus() == ParkingLotStatus.ACTIVE;
        List<PublicParkingLotDirectoryResult.VehicleTypeAvailability> vehicleTypes = new ArrayList<>();
        for (VehicleType vehicleType : availableTypes.values()) {
            AvailabilityCounters counters = countersByVehicleType.getOrDefault(
                    vehicleType.getVehicleTypeId(), new AvailabilityCounters());
            vehicleTypes.add(new PublicParkingLotDirectoryResult.VehicleTypeAvailability(
                    vehicleType.getVehicleTypeId(),
                    vehicleType.getCode(),
                    vehicleType.getName(),
                    counters.totalCapacity,
                    operational ? counters.visitorAvailable : 0,
                    operational ? counters.subscriptionAvailable : 0
            ));
        }
        vehicleTypes.sort(Comparator.comparing(
                PublicParkingLotDirectoryResult.VehicleTypeAvailability::name,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
        ));

        long totalCapacity = vehicleTypes.stream()
                .mapToLong(PublicParkingLotDirectoryResult.VehicleTypeAvailability::totalCapacity)
                .sum();
        long visitorAvailable = vehicleTypes.stream()
                .mapToLong(PublicParkingLotDirectoryResult.VehicleTypeAvailability::visitorAvailable)
                .sum();
        List<PricePlan> pricePlans = pricePlanPortOut.findAll(
                true, null, businessDate, null, Set.of(lot.getOrganizationId()));
        Set<UUID> pricePlanIds = pricePlans.stream()
                .map(PricePlan::getPricePlanId)
                .collect(java.util.stream.Collectors.toSet());
        List<PriceRule> priceRules = pricePlanIds.isEmpty()
                ? List.of()
                : publicParkingLotCatalogPortIn.filterAvailablePriceRules(
                        lot.getParkingLotId(),
                        priceRulePortOut.findAll(null, null, null, true, null, Set.of(lot.getOrganizationId()))
                                .stream()
                                .filter(rule -> pricePlanIds.contains(rule.getPricePlanId()))
                                .toList()
                );
        List<java.math.BigDecimal> configuredPrices = priceRules.stream()
                .map(PriceRule::getBasePrice)
                .filter(java.util.Objects::nonNull)
                .toList();
        java.math.BigDecimal minimumPrice = configuredPrices.stream()
                .min(java.math.BigDecimal::compareTo)
                .orElse(null);
        java.math.BigDecimal maximumPrice = configuredPrices.stream()
                .max(java.math.BigDecimal::compareTo)
                .orElse(null);
        return new PublicParkingLotDirectoryResult(
                lot.getParkingLotId(),
                lot.getCode(),
                lot.getName(),
                lot.getAddressDisplay(),
                lot.getLatitude(),
                lot.getLongitude(),
                lot.getStatus(),
                totalCapacity,
                visitorAvailable,
                operational && visitorAvailable > 0,
                minimumPrice,
                maximumPrice,
                parkingSessionPortOut.countByParkingLotIdAndStatus(
                        lot.getParkingLotId(), ParkingSessionStatus.CLOSED),
                reviewAggregate.averageRating(),
                reviewAggregate.reviewCount(),
                snapshotAt,
                vehicleTypes
        );
    }

    private static final class AvailabilityCounters {
        private long totalCapacity;
        private long visitorAvailable;
        private long subscriptionAvailable;

        private void add(ParkingAvailabilitySnapshot snapshot) {
            totalCapacity += snapshot.configuredCapacity();
            visitorAvailable += snapshot.visitorAvailable();
            subscriptionAvailable += snapshot.physicalAvailable();
        }
    }
}
