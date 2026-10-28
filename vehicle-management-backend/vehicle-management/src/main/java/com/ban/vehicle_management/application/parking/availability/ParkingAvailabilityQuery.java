package com.ban.vehicle_management.application.parking.availability;

import com.ban.vehicle_management.application.parking.capacityallocation.port.out.SubscriptionCapacityAllocationPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.parking.parkingspace.port.out.ParkingSpacePortOut;
import com.ban.vehicle_management.application.parking.parkingspaceallocation.port.out.ParkingSpaceAllocationPortOut;
import com.ban.vehicle_management.domain.parking.availability.model.ParkingAvailabilitySnapshot;
import com.ban.vehicle_management.domain.parking.availability.policy.ParkingAvailabilityPolicy;
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;
import com.ban.vehicle_management.shared.enumeration.parking.TrackingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ParkingAvailabilityQuery {
    private final ParkingSessionPortOut sessionPortOut;
    private final ParkingSpacePortOut spacePortOut;
    private final ParkingSpaceAllocationPortOut spaceAllocationPortOut;
    private final SubscriptionCapacityAllocationPortOut capacityAllocationPortOut;
    private final ParkingAvailabilityPolicy policy = new ParkingAvailabilityPolicy();

    public ParkingAvailabilityQuery(ParkingSessionPortOut sessionPortOut,
            ParkingSpacePortOut spacePortOut,
            ParkingSpaceAllocationPortOut spaceAllocationPortOut,
            SubscriptionCapacityAllocationPortOut capacityAllocationPortOut) {
        this.sessionPortOut = sessionPortOut;
        this.spacePortOut = spacePortOut;
        this.spaceAllocationPortOut = spaceAllocationPortOut;
        this.capacityAllocationPortOut = capacityAllocationPortOut;
    }

    public ParkingAvailabilitySnapshot get(Zone zone, LocalDate businessDate) {
        return zone.getTrackingMode() == TrackingMode.SPACE
                ? getSpace(zone, businessDate)
                : getCapacity(zone, businessDate);
    }

    public long maxCommittedCapacity(UUID zoneId) {
        return capacityAllocationPortOut.maxConcurrentReservations(zoneId);
    }

    private ParkingAvailabilitySnapshot getCapacity(Zone zone, LocalDate date) {
        var entitlements = capacityAllocationPortOut.findEffectiveByZoneId(zone.getZoneId(), date);
        Set<UUID> validSubscriptionIds = entitlements.stream()
                .map(item -> item.getSubscriptionId()).collect(Collectors.toSet());
        List<ParkingSession> sessions = sessionPortOut.findOpenByZoneId(zone.getZoneId());
        long registered = sessions.stream().filter(this::registered).count();
        long visitor = sessions.size() - registered;
        long registeredEntitled = sessions.stream()
                .filter(this::registered)
                .filter(item -> item.getSubscriptionId() != null
                        && validSubscriptionIds.contains(item.getSubscriptionId()))
                .count();
        return policy.calculateCapacity(zone.getZoneId(), value(zone.getCapacity()),
                value(zone.getUnavailableCapacity()), entitlements.size(), registered,
                registeredEntitled, visitor);
    }

    private ParkingAvailabilitySnapshot getSpace(Zone zone, LocalDate date) {
        List<ParkingSpace> spaces = spacePortOut.findByZoneIdAndLifecycleStatus(zone.getZoneId(), "ACTIVE");
        List<UUID> ids = spaces.stream().map(ParkingSpace::getParkingSpaceId).toList();
        Set<UUID> allocated = ids.isEmpty() ? Set.of()
                : spaceAllocationPortOut.findActiveAllocationsForSpacesOnDate(ids, date).stream()
                        .map(item -> item.getParkingSpaceId()).collect(Collectors.toSet());
        List<ParkingSession> sessions = sessionPortOut.findOpenByParkingSpaceIds(ids);
        long registered = sessions.stream().filter(this::registered).count();
        long visitor = sessions.size() - registered;
        Set<UUID> occupiedIds = sessions.stream().map(ParkingSession::getParkingSpaceId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        long available = spaces.stream().filter(space -> space.getStatus() == ParkingSpaceStatus.AVAILABLE)
                .filter(space -> !allocated.contains(space.getParkingSpaceId()))
                .filter(space -> !occupiedIds.contains(space.getParkingSpaceId())).count();
        long reserved = spaces.stream().filter(space -> space.getStatus() == ParkingSpaceStatus.RESERVED).count();
        long maintenance = spaces.stream().filter(space -> space.getStatus() == ParkingSpaceStatus.MAINTENANCE).count();
        return policy.calculateSpace(zone.getZoneId(), spaces.size(), available, reserved,
                registered, visitor, maintenance, allocated.size());
    }

    private boolean registered(ParkingSession session) {
        return session.getSubscriptionId() != null || session.getCustomerVehicleId() != null;
    }

    private long value(Integer number) {
        return number == null ? 0 : number.longValue();
    }
}
