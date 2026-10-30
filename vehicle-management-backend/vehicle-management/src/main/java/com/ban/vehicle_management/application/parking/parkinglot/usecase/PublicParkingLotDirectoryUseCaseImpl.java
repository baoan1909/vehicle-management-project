package com.ban.vehicle_management.application.parking.parkinglot.usecase;

import com.ban.vehicle_management.application.catalog.availability.port.in.PublicParkingLotCatalogPortIn;
import com.ban.vehicle_management.application.parking.availability.ParkingAvailabilityQuery;
import com.ban.vehicle_management.application.parking.parkinglot.model.result.PublicParkingLotDirectoryResult;
import com.ban.vehicle_management.application.parking.parkinglot.port.in.PublicParkingLotDirectoryPortIn;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import com.ban.vehicle_management.domain.parking.availability.model.ParkingAvailabilitySnapshot;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicParkingLotDirectoryUseCaseImpl implements PublicParkingLotDirectoryPortIn {

    private static final List<ParkingLotStatus> PUBLIC_STATUSES = List.of(
            ParkingLotStatus.ACTIVE,
            ParkingLotStatus.SETUP,
            ParkingLotStatus.MAINTENANCE
    );

    private final ParkingLotPortOut parkingLotPortOut;
    private final ZonePortOut zonePortOut;
    private final PublicParkingLotCatalogPortIn publicParkingLotCatalogPortIn;
    private final ParkingAvailabilityQuery parkingAvailabilityQuery;

    public PublicParkingLotDirectoryUseCaseImpl(
            ParkingLotPortOut parkingLotPortOut,
            ZonePortOut zonePortOut,
            PublicParkingLotCatalogPortIn publicParkingLotCatalogPortIn,
            ParkingAvailabilityQuery parkingAvailabilityQuery
    ) {
        this.parkingLotPortOut = parkingLotPortOut;
        this.zonePortOut = zonePortOut;
        this.publicParkingLotCatalogPortIn = publicParkingLotCatalogPortIn;
        this.parkingAvailabilityQuery = parkingAvailabilityQuery;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicParkingLotDirectoryResult> getDirectory() {
        Instant snapshotAt = Instant.now();
        LocalDate businessDate = DateTimeUtils.toAppLocalDate(snapshotAt);
        return PUBLIC_STATUSES.stream()
                .flatMap(status -> parkingLotPortOut.findAll(status, null, null, null).stream())
                .sorted(Comparator
                        .comparingInt((ParkingLot lot) -> statusOrder(lot.getStatus()))
                        .thenComparing(ParkingLot::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(lot -> toResult(lot, businessDate, snapshotAt))
                .toList();
    }

    private PublicParkingLotDirectoryResult toResult(
            ParkingLot lot,
            LocalDate businessDate,
            Instant snapshotAt
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
        for (Map.Entry<UUID, AvailabilityCounters> entry : countersByVehicleType.entrySet()) {
            VehicleType vehicleType = availableTypes.get(entry.getKey());
            if (vehicleType == null) {
                continue;
            }
            AvailabilityCounters counters = entry.getValue();
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
                snapshotAt,
                vehicleTypes
        );
    }

    private int statusOrder(ParkingLotStatus status) {
        if (status == ParkingLotStatus.ACTIVE) return 0;
        if (status == ParkingLotStatus.SETUP) return 1;
        if (status == ParkingLotStatus.MAINTENANCE) return 2;
        return 3;
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
