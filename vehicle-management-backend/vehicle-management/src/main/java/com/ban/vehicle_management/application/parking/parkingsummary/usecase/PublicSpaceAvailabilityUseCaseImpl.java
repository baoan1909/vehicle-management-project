package com.ban.vehicle_management.application.parking.parkingsummary.usecase;

import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.availability.ParkingAvailabilityQuery;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.parking.parkingspace.port.out.ParkingSpacePortOut;
import com.ban.vehicle_management.application.parking.parkingspaceallocation.port.out.ParkingSpaceAllocationPortOut;
import com.ban.vehicle_management.application.parking.parkingsummary.model.result.PublicAvailabilityResult;
import com.ban.vehicle_management.application.parking.parkingsummary.port.in.PublicSpaceAvailabilityPortIn;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;
import com.ban.vehicle_management.shared.enumeration.parking.TrackingMode;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicSpaceAvailabilityUseCaseImpl implements PublicSpaceAvailabilityPortIn {

    private final ParkingLotPortOut parkingLotPortOut;
    private final ZonePortOut zonePortOut;
    private final ParkingSpacePortOut parkingSpacePortOut;
    private final ParkingSpaceAllocationPortOut allocationPortOut;
    private final ParkingSessionPortOut parkingSessionPortOut;
    private final ParkingAvailabilityQuery availabilityQuery;

    @Autowired
    public PublicSpaceAvailabilityUseCaseImpl(
            ParkingLotPortOut parkingLotPortOut,
            ZonePortOut zonePortOut,
            ParkingSpacePortOut parkingSpacePortOut,
            ParkingSpaceAllocationPortOut allocationPortOut,
            ParkingSessionPortOut parkingSessionPortOut,
            ParkingAvailabilityQuery availabilityQuery
    ) {
        this.parkingLotPortOut = parkingLotPortOut;
        this.zonePortOut = zonePortOut;
        this.parkingSpacePortOut = parkingSpacePortOut;
        this.allocationPortOut = allocationPortOut;
        this.parkingSessionPortOut = parkingSessionPortOut;
        this.availabilityQuery = availabilityQuery;
    }

    public PublicSpaceAvailabilityUseCaseImpl(ParkingLotPortOut parkingLotPortOut,
            ZonePortOut zonePortOut, ParkingSpacePortOut parkingSpacePortOut,
            ParkingSpaceAllocationPortOut allocationPortOut, ParkingSessionPortOut parkingSessionPortOut) {
        this(parkingLotPortOut, zonePortOut, parkingSpacePortOut, allocationPortOut,
                parkingSessionPortOut, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicAvailabilityResult getPublicAvailability(UUID parkingLotId) {
        if (parkingLotId == null) {
            throw new NotFoundException("Parking lot not found");
        }
        ParkingLot lot = parkingLotPortOut.findById(parkingLotId)
                .orElseThrow(() -> new NotFoundException("Parking lot not found"));
        if (lot.getStatus() != ParkingLotStatus.ACTIVE) {
            throw new NotFoundException("Parking lot not found");
        }
        LocalDate today = DateTimeUtils.toAppLocalDate(Instant.now());
        List<Zone> zones = zonePortOut.findAll(lot.getParkingLotId(), null, null, null);
        List<ParkingSpace> spaces = new ArrayList<>();
        for (Zone zone : zones) {
            if (zone.getTrackingMode() == TrackingMode.SPACE) {
                spaces.addAll(parkingSpacePortOut.findByZoneIdAndLifecycleStatus(
                        zone.getZoneId(), "ACTIVE"));
            }
        }
        List<UUID> spaceIds = spaces.stream().map(ParkingSpace::getParkingSpaceId).toList();
        Set<UUID> allocated = spaceIds.isEmpty() ? Set.of()
                : allocationPortOut.findActiveAllocationsForSpacesOnDate(spaceIds, today).stream()
                        .map(allocation -> allocation.getParkingSpaceId())
                        .collect(Collectors.toUnmodifiableSet());
        Set<UUID> withOpenSession = spaceIds.isEmpty() ? Set.of()
                : parkingSessionPortOut.findOpenByParkingSpaceIds(spaceIds).stream()
                        .map(session -> session.getParkingSpaceId())
                        .collect(Collectors.toUnmodifiableSet());

        long visitorAvailable = 0;
        Map<UUID, long[]> perType = new LinkedHashMap<>();
        for (ParkingSpace space : spaces) {
            boolean physical = space.getStatus() == ParkingSpaceStatus.AVAILABLE
                    && !withOpenSession.contains(space.getParkingSpaceId());
            boolean visitor = physical && !allocated.contains(space.getParkingSpaceId());
            if (visitor) {
                visitorAvailable++;
            }
            long[] counters = perType.computeIfAbsent(space.getVehicleTypeId(), key -> new long[3]);
            counters[0]++;
            if (visitor) {
                counters[1]++;
                counters[2]++;
            }
        }
        List<PublicAvailabilityResult.VehicleTypeAvailability> breakdown = new ArrayList<>();
        for (Map.Entry<UUID, long[]> entry : perType.entrySet()) {
            breakdown.add(new PublicAvailabilityResult.VehicleTypeAvailability(
                    entry.getKey(), entry.getValue()[0], entry.getValue()[1], entry.getValue()[2]));
        }
        long configuredCapacity = spaces.size();
        long unavailableCapacity = 0;
        long effectiveCapacity = spaces.size();
        long subscriptionReserved = allocated.size();
        long occupiedRegistered = 0;
        long occupiedVisitor = 0;
        long physicalAvailable = visitorAvailable;
        if (availabilityQuery != null) {
            var snapshots = zones.stream().map(zone -> availabilityQuery.get(zone, today)).toList();
            configuredCapacity = snapshots.stream().mapToLong(item -> item.configuredCapacity()).sum();
            unavailableCapacity = snapshots.stream().mapToLong(item -> item.unavailableCapacity()).sum();
            effectiveCapacity = snapshots.stream().mapToLong(item -> item.effectiveCapacity()).sum();
            subscriptionReserved = snapshots.stream().mapToLong(item -> item.subscriptionReserved()).sum();
            occupiedRegistered = snapshots.stream().mapToLong(item -> item.occupiedRegistered()).sum();
            occupiedVisitor = snapshots.stream().mapToLong(item -> item.occupiedVisitor()).sum();
            physicalAvailable = snapshots.stream().mapToLong(item -> item.physicalAvailable()).sum();
            visitorAvailable = snapshots.stream().mapToLong(item -> item.visitorAvailable()).sum();
        }
        return new PublicAvailabilityResult(
                lot.getParkingLotId(),
                lot.getName(),
                lot.getStatus() == null ? null : lot.getStatus().name(),
                configuredCapacity,
                unavailableCapacity,
                effectiveCapacity,
                subscriptionReserved,
                occupiedRegistered,
                occupiedVisitor,
                physicalAvailable,
                configuredCapacity,
                visitorAvailable,
                physicalAvailable,
                visitorAvailable > 0,
                Instant.now(),
                breakdown);
    }
}
