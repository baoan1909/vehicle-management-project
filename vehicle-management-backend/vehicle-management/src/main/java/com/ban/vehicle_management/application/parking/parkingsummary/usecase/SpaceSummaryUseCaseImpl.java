package com.ban.vehicle_management.application.parking.parkingsummary.usecase;

import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglevel.port.out.ParkingLevelPortOut;
import com.ban.vehicle_management.application.parking.availability.ParkingAvailabilityQuery;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.parking.parkingspace.port.out.ParkingSpacePortOut;
import com.ban.vehicle_management.application.parking.parkingspaceallocation.port.out.ParkingSpaceAllocationPortOut;
import com.ban.vehicle_management.application.parking.parkingsummary.model.result.LevelMapResult;
import com.ban.vehicle_management.application.parking.parkingsummary.model.result.SpaceReconciliationIssue;
import com.ban.vehicle_management.application.parking.parkingsummary.model.result.SpaceSummaryResult;
import com.ban.vehicle_management.application.parking.parkingsummary.port.in.SpaceSummaryPortIn;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.domain.parking.parkinglevel.model.ParkingLevel;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.enumeration.parking.ManualOccupancyType;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;
import com.ban.vehicle_management.shared.enumeration.parking.TrackingMode;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpaceSummaryUseCaseImpl implements SpaceSummaryPortIn {

    private final ZonePortOut zonePortOut;
    private final ParkingLevelPortOut parkingLevelPortOut;
    private final ParkingLotPortOut parkingLotPortOut;
    private final ParkingSpacePortOut parkingSpacePortOut;
    private final ParkingSpaceAllocationPortOut allocationPortOut;
    private final ParkingSessionPortOut parkingSessionPortOut;
    private final OrganizationAccessGuard organizationAccessGuard;
    private final ParkingAvailabilityQuery availabilityQuery;

    @Autowired
    public SpaceSummaryUseCaseImpl(
            ZonePortOut zonePortOut,
            ParkingLevelPortOut parkingLevelPortOut,
            ParkingLotPortOut parkingLotPortOut,
            ParkingSpacePortOut parkingSpacePortOut,
            ParkingSpaceAllocationPortOut allocationPortOut,
            ParkingSessionPortOut parkingSessionPortOut,
            OrganizationAccessGuard organizationAccessGuard,
            ParkingAvailabilityQuery availabilityQuery
    ) {
        this.zonePortOut = zonePortOut;
        this.parkingLevelPortOut = parkingLevelPortOut;
        this.parkingLotPortOut = parkingLotPortOut;
        this.parkingSpacePortOut = parkingSpacePortOut;
        this.allocationPortOut = allocationPortOut;
        this.parkingSessionPortOut = parkingSessionPortOut;
        this.organizationAccessGuard = organizationAccessGuard;
        this.availabilityQuery = availabilityQuery;
    }

    public SpaceSummaryUseCaseImpl(ZonePortOut zonePortOut, ParkingLevelPortOut parkingLevelPortOut,
            ParkingLotPortOut parkingLotPortOut, ParkingSpacePortOut parkingSpacePortOut,
            ParkingSpaceAllocationPortOut allocationPortOut, ParkingSessionPortOut parkingSessionPortOut,
            OrganizationAccessGuard organizationAccessGuard) {
        this(zonePortOut, parkingLevelPortOut, parkingLotPortOut, parkingSpacePortOut,
                allocationPortOut, parkingSessionPortOut, organizationAccessGuard, null);
    }

    @Override
    @Transactional(readOnly = true)
    public SpaceSummaryResult getZoneSummary(UUID zoneId) {
        Zone zone = findAccessibleZone(zoneId);
        return summarizeZone(zone, today());
    }

    @Override
    @Transactional(readOnly = true)
    public SpaceSummaryResult getLevelSummary(UUID levelId) {
        ParkingLevel level = parkingLevelPortOut.findById(levelId)
                .orElseThrow(() -> new NotFoundException("Parking level not found"));
        ensureCanAccessParkingLot(level.getParkingLotId());
        List<Zone> zones = zonePortOut.findAll(level.getParkingLotId(), null, null, null).stream()
                .filter(zone -> levelId.equals(zone.getParkingLevelId()))
                .toList();
        return aggregate(levelId, "LEVEL", zones, today());
    }

    @Override
    @Transactional(readOnly = true)
    public SpaceSummaryResult getLotSummary(UUID parkingLotId) {
        ensureCanAccessParkingLot(parkingLotId);
        List<Zone> zones = zonePortOut.findAll(parkingLotId, null, null, null);
        return aggregate(parkingLotId, "PARKING_LOT", zones, today());
    }

    @Override
    @Transactional(readOnly = true)
    public LevelMapResult getLevelMap(UUID levelId) {
        ParkingLevel level = parkingLevelPortOut.findById(levelId)
                .orElseThrow(() -> new NotFoundException("Parking level not found"));
        ensureCanAccessParkingLot(level.getParkingLotId());
        List<Zone> zones = zonePortOut.findAll(level.getParkingLotId(), null, null, null).stream()
                .filter(zone -> levelId.equals(zone.getParkingLevelId()))
                .toList();
        LocalDate date = today();
        List<ParkingSpace> spaces = zones.stream()
                .filter(zone -> zone.getTrackingMode() == TrackingMode.SPACE)
                .flatMap(zone -> parkingSpacePortOut
                        .findByZoneIdAndLifecycleStatus(zone.getZoneId(), "ACTIVE").stream())
                .toList();
        return new LevelMapResult(level, zones, spaces, aggregate(levelId, "LEVEL", zones, date));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpaceReconciliationIssue> findZoneReconciliationIssues(UUID zoneId) {
        Zone zone = findAccessibleZone(zoneId);
        if (zone.getTrackingMode() != TrackingMode.SPACE) {
            return List.of();
        }
        return reconcileZone(zone, today());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpaceReconciliationIssue> findLotReconciliationIssues(UUID parkingLotId) {
        ensureCanAccessParkingLot(parkingLotId);
        List<SpaceReconciliationIssue> issues = new ArrayList<>();
        LocalDate date = today();
        for (Zone zone : zonePortOut.findAll(parkingLotId, null, null, null)) {
            if (zone.getTrackingMode() == TrackingMode.SPACE) {
                issues.addAll(reconcileZone(zone, date));
            }
        }
        return issues;
    }

    private SpaceSummaryResult summarizeZone(Zone zone, LocalDate date) {
        if (availabilityQuery != null) {
            var value = availabilityQuery.get(zone, date);
            long unknown = Math.max(0, value.occupiedSpaceCount()
                    - value.occupiedRegistered() - value.occupiedVisitor());
            return new SpaceSummaryResult(zone.getZoneId(), "ZONE",
                    value.configuredCapacity(), value.visitorAvailable(),
                    value.occupiedRegistered() + value.occupiedVisitor(), value.occupiedVisitor(),
                    value.occupiedRegistered(), unknown, value.reservedSpaceCount(),
                    value.maintenanceSpaceCount(), value.subscriptionReserved(),
                    value.configuredCapacity(), value.unavailableCapacity(), value.effectiveCapacity(),
                    value.subscriptionReserved(), value.physicalAvailable(), value.visitorAvailable(),
                    List.of(), Instant.now());
        }
        if (zone.getTrackingMode() != TrackingMode.SPACE) {
            long open = zonePortOut.countOpenSessions(zone.getZoneId());
            long capacity = zone.getCapacity() == null ? 0 : zone.getCapacity();
            return new SpaceSummaryResult(
                    zone.getZoneId(), "ZONE", capacity, Math.max(0, capacity - open), open,
                    0, 0, open, 0, 0, 0,
                    capacity, 0, capacity, 0, Math.max(0, capacity - open),
                    Math.max(0, capacity - open), List.of(), Instant.now());
        }
        List<ParkingSpace> spaces = parkingSpacePortOut
                .findByZoneIdAndLifecycleStatus(zone.getZoneId(), "ACTIVE");
        long available = 0;
        long visitor = 0;
        long registered = 0;
        long unknown = 0;
        long reserved = 0;
        long maintenance = 0;
        Set<UUID> allocatedSpaces = allocatedSpaceIds(spaces, date);
        Map<UUID, ParkingSession> openSessionBySpace = openSessionBySpaceId(spaces);
        Set<UUID> spacesWithOpenSession = openSessionBySpace.keySet();
        for (ParkingSpace space : spaces) {
            switch (space.getStatus()) {
                case AVAILABLE -> {
                    if (!allocatedSpaces.contains(space.getParkingSpaceId())
                            && !spacesWithOpenSession.contains(space.getParkingSpaceId())) {
                        available++;
                    }
                }
                case OCCUPIED -> {
                    if (space.getManualOccupancyType() == ManualOccupancyType.VISITOR) {
                        visitor++;
                    } else if (space.getManualOccupancyType() == ManualOccupancyType.REGISTERED) {
                        registered++;
                    } else {
                        ParkingSession open = openSessionBySpace.get(space.getParkingSpaceId());
                        if (open != null
                                && (open.getCustomerId() != null || open.getCustomerVehicleId() != null)) {
                            registered++;
                        } else if (open != null) {
                            visitor++;
                        } else {
                            unknown++;
                        }
                    }
                }
                case RESERVED -> reserved++;
                case MAINTENANCE -> maintenance++;
            }
        }
        long occupied = visitor + registered + unknown;
        return new SpaceSummaryResult(
                zone.getZoneId(), "ZONE", spaces.size(), available, occupied,
                visitor, registered, unknown, reserved, maintenance,
                allocatedSpaces.size(), spaces.size(), maintenance,
                Math.max(0, spaces.size() - maintenance), allocatedSpaces.size(),
                Math.max(0, spaces.size() - maintenance - occupied), available,
                List.of(), Instant.now());
    }

    private SpaceSummaryResult aggregate(UUID scopeId, String scopeType, List<Zone> zones, LocalDate date) {
        List<SpaceSummaryResult> breakdown = zones.stream().map(zone -> summarizeZone(zone, date)).toList();
        return new SpaceSummaryResult(
                scopeId, scopeType,
                sum(breakdown, SpaceSummaryResult::total),
                sum(breakdown, SpaceSummaryResult::available),
                sum(breakdown, SpaceSummaryResult::occupied),
                sum(breakdown, SpaceSummaryResult::occupiedVisitor),
                sum(breakdown, SpaceSummaryResult::occupiedRegistered),
                sum(breakdown, SpaceSummaryResult::occupiedUnknown),
                sum(breakdown, SpaceSummaryResult::reserved),
                sum(breakdown, SpaceSummaryResult::maintenance),
                sum(breakdown, SpaceSummaryResult::subscriptionAllocated),
                sum(breakdown, SpaceSummaryResult::configuredCapacity),
                sum(breakdown, SpaceSummaryResult::unavailableCapacity),
                sum(breakdown, SpaceSummaryResult::effectiveCapacity),
                sum(breakdown, SpaceSummaryResult::subscriptionReserved),
                sum(breakdown, SpaceSummaryResult::physicalAvailable),
                sum(breakdown, SpaceSummaryResult::visitorAvailable),
                breakdown, Instant.now());
    }

    private long sum(List<SpaceSummaryResult> results,
            java.util.function.ToLongFunction<SpaceSummaryResult> selector) {
        return results.stream().mapToLong(selector).sum();
    }

    private Set<UUID> allocatedSpaceIds(List<ParkingSpace> spaces, LocalDate date) {
        List<UUID> spaceIds = spaces.stream().map(ParkingSpace::getParkingSpaceId).toList();
        if (spaceIds.isEmpty()) {
            return Set.of();
        }
        return allocationPortOut.findActiveAllocationsForSpacesOnDate(spaceIds, date).stream()
                .map(allocation -> allocation.getParkingSpaceId())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private Set<UUID> openSessionSpaceIds(List<ParkingSpace> spaces) {
        return openSessionBySpaceId(spaces).keySet();
    }

    private Map<UUID, ParkingSession> openSessionBySpaceId(List<ParkingSpace> spaces) {
        List<UUID> spaceIds = spaces.stream().map(ParkingSpace::getParkingSpaceId).toList();
        if (spaceIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, ParkingSession> bySpace = new java.util.HashMap<>();
        for (ParkingSession session : parkingSessionPortOut.findOpenByParkingSpaceIds(spaceIds)) {
            if (session.getParkingSpaceId() != null) {
                bySpace.put(session.getParkingSpaceId(), session);
            }
        }
        return java.util.Collections.unmodifiableMap(bySpace);
    }

    private List<SpaceReconciliationIssue> reconcileZone(Zone zone, LocalDate date) {
        List<SpaceReconciliationIssue> issues = new ArrayList<>();
        List<ParkingSpace> spaces = parkingSpacePortOut
                .findByZoneIdAndLifecycleStatus(zone.getZoneId(), "ACTIVE");
        for (ParkingSpace space : spaces) {
            var openSession = parkingSessionPortOut.findOpenByParkingSpaceId(space.getParkingSpaceId());
            if (space.getStatus() == ParkingSpaceStatus.OCCUPIED && openSession.isEmpty()) {
                issues.add(new SpaceReconciliationIssue(
                        "OCCUPIED_WITHOUT_OPEN_SESSION",
                        space.getParkingSpaceId(), null, null,
                        "Space " + space.getCode() + " is OCCUPIED without an open session"));
            }
            if (openSession.isPresent() && space.getStatus() != ParkingSpaceStatus.OCCUPIED) {
                issues.add(new SpaceReconciliationIssue(
                        "OPEN_SESSION_SPACE_NOT_OCCUPIED",
                        space.getParkingSpaceId(), openSession.get().getParkingSessionId(), null,
                        "Open session exists but space " + space.getCode() + " is " + space.getStatus()));
            }
            if (openSession.isPresent()) {
                ParkingSession session = openSession.get();
                if (!zone.getZoneId().equals(session.getZoneId())
                        || !zone.getParkingLotId().equals(session.getParkingLotId())) {
                    issues.add(new SpaceReconciliationIssue(
                            "SESSION_SPACE_TOPOLOGY_MISMATCH",
                            space.getParkingSpaceId(), session.getParkingSessionId(), null,
                            "Session zone/lot differs from space " + space.getCode()));
                }
            }
            boolean hasAllocation = !allocationPortOut.findActiveAllocationsForSpaceOnDate(
                    space.getParkingSpaceId(), date).isEmpty();
            if (space.getStatus() == ParkingSpaceStatus.RESERVED && !hasAllocation) {
                issues.add(new SpaceReconciliationIssue(
                        "RESERVED_WITHOUT_ALLOCATION",
                        space.getParkingSpaceId(), null, null,
                        "Space " + space.getCode() + " is RESERVED without an active allocation"));
            }
            if (space.getStatus() == ParkingSpaceStatus.AVAILABLE && hasAllocation) {
                issues.add(new SpaceReconciliationIssue(
                        "ALLOCATION_SPACE_AVAILABLE",
                        space.getParkingSpaceId(), null, null,
                        "Space " + space.getCode() + " is AVAILABLE despite an active allocation"));
            }
        }        return issues;
    }

    private Zone findAccessibleZone(UUID zoneId) {
        if (zoneId == null) {
            throw new NotFoundException("Zone not found");
        }
        Zone zone = zonePortOut.findById(zoneId)
                .orElseThrow(() -> new NotFoundException("Zone not found"));
        ensureCanAccessParkingLot(zone.getParkingLotId());
        return zone;
    }

    private void ensureCanAccessParkingLot(UUID parkingLotId) {
        if (parkingLotId == null) {
            throw new NotFoundException("Parking lot not found");
        }
        ParkingLot lot = parkingLotPortOut.findById(parkingLotId)
                .orElseThrow(() -> new NotFoundException("Parking lot not found"));
        organizationAccessGuard.ensureCanAccessParkingLot(lot);
    }

    private LocalDate today() {
        return DateTimeUtils.toAppLocalDate(Instant.now());
    }
}
