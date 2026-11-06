package com.ban.vehicle_management.application.parking.allocation;

import com.ban.vehicle_management.application.parking.capacityallocation.port.out.SubscriptionCapacityAllocationPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.parking.parkingspace.port.out.ParkingSpacePortOut;
import com.ban.vehicle_management.application.parking.parkingspaceallocation.port.out.ParkingSpaceAllocationPortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.application.accesscontrol.subscription.model.result.SubscriptionSpaceOption;
import com.ban.vehicle_management.application.accesscontrol.subscription.model.result.SubscriptionSpaceSelection;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.parking.capacityallocation.model.SubscriptionCapacityAllocation;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.domain.parking.parkingspace.policy.ParkingSpacePolicy;
import com.ban.vehicle_management.domain.parking.parkingspaceallocation.model.ParkingSpaceAllocation;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceAllocationStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ManualOccupancyType;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;
import com.ban.vehicle_management.shared.enumeration.parking.SubscriptionCapacityAllocationStatus;
import com.ban.vehicle_management.shared.enumeration.parking.TrackingMode;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates subscription reservation across CAPACITY and SPACE zones.
 * Lock order: subscription (caller) -> zone -> space -> card -> session.
 */
@Service
public class SubscriptionParkingAllocationService {
    private final ZonePortOut zonePortOut;
    private final ParkingSpacePortOut spacePortOut;
    private final ParkingSpaceAllocationPortOut spaceAllocationPortOut;
    private final SubscriptionCapacityAllocationPortOut capacityAllocationPortOut;
    private final ParkingSessionPortOut sessionPortOut;
    private final ParkingSpacePolicy spacePolicy = new ParkingSpacePolicy();

    public SubscriptionParkingAllocationService(ZonePortOut zonePortOut,
            ParkingSpacePortOut spacePortOut,
            ParkingSpaceAllocationPortOut spaceAllocationPortOut,
            SubscriptionCapacityAllocationPortOut capacityAllocationPortOut,
            ParkingSessionPortOut sessionPortOut) {
        this.zonePortOut = zonePortOut;
        this.spacePortOut = spacePortOut;
        this.spaceAllocationPortOut = spaceAllocationPortOut;
        this.capacityAllocationPortOut = capacityAllocationPortOut;
        this.sessionPortOut = sessionPortOut;
    }

    public TrackingMode holdForApproval(Subscription subscription, UUID vehicleTypeId, Instant holdExpiresAt) {
        if (capacityAllocationPortOut.findLiveBySubscriptionId(subscription.getSubscriptionId()).isPresent()
                || !spaceAllocationPortOut.findActiveAllocationsBySubscription(
                        subscription.getSubscriptionId()).isEmpty()) {
            return capacityAllocationPortOut.findLiveBySubscriptionId(subscription.getSubscriptionId()).isPresent()
                    ? TrackingMode.CAPACITY : TrackingMode.SPACE;
        }
        List<Zone> candidates = zonePortOut.findAll(subscription.getParkingLotId(), vehicleTypeId, null, null)
                .stream()
                .filter(zone -> zone.getStatus() == ZoneStatus.ACTIVE)
                .filter(zone -> accepts(zone, vehicleTypeId))
                .sorted(Comparator
                        .comparingInt(this::supportedVehicleTypeCount)
                        .thenComparing(Zone::getCode, Comparator.nullsLast(String::compareTo)))
                .toList();
        for (Zone candidate : candidates) {
            Zone zone = zonePortOut.findByIdForUpdate(candidate.getZoneId()).orElse(null);
            if (zone == null) {
                continue;
            }
            if (zone.getTrackingMode() == TrackingMode.CAPACITY
                    && tryHoldCapacity(subscription, vehicleTypeId, holdExpiresAt, zone)) {
                return TrackingMode.CAPACITY;
            }
            if (zone.getTrackingMode() == TrackingMode.SPACE
                    && tryHoldSpace(subscription, vehicleTypeId, holdExpiresAt, zone)) {
                return TrackingMode.SPACE;
            }
        }
        throw new ConflictException("No compatible subscription capacity or parking space is available");
    }

    public SubscriptionSpaceSelection getSpaceSelection(Subscription subscription, UUID vehicleTypeId) {
        ParkingSpaceAllocation allocation = requireSpaceAllocation(subscription.getSubscriptionId());
        ParkingSpace selected = spacePortOut.findById(allocation.getParkingSpaceId())
                .orElseThrow(() -> new ConflictException("Allocated parking space no longer exists"));
        Zone zone = zonePortOut.findById(selected.getZoneId())
                .orElseThrow(() -> new ConflictException("Allocated parking zone no longer exists"));

        List<SubscriptionSpaceOption> options = spacePortOut
                .findByZoneIdAndLifecycleStatus(zone.getZoneId(), "ACTIVE")
                .stream()
                .sorted(Comparator.comparing(ParkingSpace::getCode, Comparator.nullsLast(String::compareTo)))
                .filter(space -> space.getVehicleTypeId() == null
                        || space.getVehicleTypeId().equals(vehicleTypeId))
                .map(space -> toOption(subscription, allocation, space))
                .toList();

        BigDecimal canvasWidth = options.stream()
                .map(option -> value(option.x()).add(value(option.width())))
                .max(BigDecimal::compareTo).orElse(BigDecimal.valueOf(1000));
        BigDecimal canvasHeight = options.stream()
                .map(option -> value(option.y()).add(value(option.height())))
                .max(BigDecimal::compareTo).orElse(BigDecimal.valueOf(700));
        return new SubscriptionSpaceSelection(
                subscription.getSubscriptionId(),
                selected.getParkingSpaceId(),
                selected.getCode(),
                canvasWidth.max(BigDecimal.ONE),
                canvasHeight.max(BigDecimal.ONE),
                subscription.getSpaceSelectionExpiresAt(),
                options
        );
    }

    public void changeHeldSpace(Subscription subscription, UUID targetParkingSpaceId, UUID vehicleTypeId) {
        ParkingSpaceAllocation allocation = requireHeldSpaceAllocation(subscription.getSubscriptionId());
        ParkingSpace current = spacePortOut.findById(allocation.getParkingSpaceId())
                .orElseThrow(() -> new ConflictException("Allocated parking space no longer exists"));
        zonePortOut.findByIdForUpdate(current.getZoneId())
                .orElseThrow(() -> new ConflictException("Allocated parking zone no longer exists"));
        ParkingSpace target = spacePortOut.findByIdForUpdate(targetParkingSpaceId)
                .orElseThrow(() -> new ConflictException("Selected parking space no longer exists"));
        if (!current.getZoneId().equals(target.getZoneId())) {
            throw new ConflictException("Selected parking space is outside the assigned subscription area");
        }
        if (target.getVehicleTypeId() != null && !target.getVehicleTypeId().equals(vehicleTypeId)) {
            throw new ConflictException("Selected parking space does not support the registered vehicle type");
        }
        if (isUnavailableByStatus(target)
                || !"ACTIVE".equalsIgnoreCase(target.getLifecycleStatus())
                || sessionPortOut.existsOpenByParkingSpaceId(targetParkingSpaceId)
                || spaceAllocationPortOut.existsOverlappingAllocation(targetParkingSpaceId,
                        subscription.getEffectiveFrom(), subscription.getEffectiveTo(), allocation.getAllocationId())) {
            throw new ConflictException("Selected parking space is no longer available");
        }
        if (targetParkingSpaceId.equals(current.getParkingSpaceId())) {
            return;
        }

        UUID previousSpaceId = allocation.getParkingSpaceId();
        allocation.setParkingSpaceId(targetParkingSpaceId);
        try {
            spaceAllocationPortOut.save(allocation);
        } catch (DataIntegrityViolationException conflict) {
            throw new ConflictException("Selected parking space was reserved concurrently");
        }
        spacePolicy.setAutomaticStatus(target, ParkingSpaceStatus.RESERVED, null);
        spacePortOut.save(target);
        recomputeSpace(previousSpaceId);
    }

    public void extendSpaceHold(UUID subscriptionId, Instant expiresAt) {
        ParkingSpaceAllocation allocation = requireHeldSpaceAllocation(subscriptionId);
        allocation.setHoldExpiresAt(expiresAt);
        spaceAllocationPortOut.save(allocation);
    }

    public TrackingMode allocationMode(UUID subscriptionId) {
        if (capacityAllocationPortOut.findLiveBySubscriptionId(subscriptionId).isPresent()) {
            return TrackingMode.CAPACITY;
        }
        if (!spaceAllocationPortOut.findActiveAllocationsBySubscription(subscriptionId).isEmpty()) {
            return TrackingMode.SPACE;
        }
        throw new ConflictException("Subscription has no parking entitlement");
    }

    public void activate(Subscription subscription) {
        UUID subscriptionId = subscription.getSubscriptionId();
        var capacityAllocation = capacityAllocationPortOut.findLiveBySubscriptionId(subscriptionId);
        List<ParkingSpaceAllocation> spaceAllocations =
                spaceAllocationPortOut.findActiveAllocationsBySubscription(subscriptionId);
        if (capacityAllocation.isEmpty() && spaceAllocations.isEmpty()) {
            throw new ConflictException("Subscription cannot become active without a parking entitlement");
        }
        capacityAllocation.ifPresent(allocation -> {
            Zone zone = zonePortOut.findByIdForUpdate(allocation.getZoneId())
                    .orElseThrow(() -> new ConflictException("Allocated capacity zone no longer exists"));
            long effectiveCapacity = Math.max(0,
                    value(zone.getCapacity()) - value(zone.getUnavailableCapacity()));
            long otherReservations = capacityAllocationPortOut.maxConcurrentReservations(
                    zone.getZoneId(), subscription.getEffectiveFrom(), subscription.getEffectiveTo(),
                    allocation.getAllocationId());
            if (otherReservations >= effectiveCapacity) {
                throw new ConflictException("Subscription capacity is unavailable for the activated period");
            }
            allocation.setEffectiveFrom(subscription.getEffectiveFrom());
            allocation.setEffectiveTo(subscription.getEffectiveTo());
            if (allocation.getAllocationStatus() == SubscriptionCapacityAllocationStatus.HELD) {
                allocation.setAllocationStatus(SubscriptionCapacityAllocationStatus.ACTIVE);
                allocation.setHoldExpiresAt(null);
            }
            capacityAllocationPortOut.save(allocation);
        });
        for (ParkingSpaceAllocation allocation : spaceAllocations) {
            ParkingSpace initialSpace = spacePortOut.findById(allocation.getParkingSpaceId())
                    .orElseThrow(() -> new ConflictException("Allocated parking space no longer exists"));
            zonePortOut.findByIdForUpdate(initialSpace.getZoneId())
                    .orElseThrow(() -> new ConflictException("Allocated parking zone no longer exists"));
            spacePortOut.findByIdForUpdate(allocation.getParkingSpaceId())
                    .orElseThrow(() -> new ConflictException("Allocated parking space no longer exists"));
            if (spaceAllocationPortOut.existsOverlappingAllocation(allocation.getParkingSpaceId(),
                    subscription.getEffectiveFrom(), subscription.getEffectiveTo(), allocation.getAllocationId())) {
                throw new ConflictException("Allocated parking space conflicts with the activated period");
            }
            allocation.setEffectiveFrom(subscription.getEffectiveFrom());
            allocation.setEffectiveTo(subscription.getEffectiveTo());
            if (allocation.getAllocationStatus() == ParkingSpaceAllocationStatus.HELD) {
                allocation.setAllocationStatus(ParkingSpaceAllocationStatus.ACTIVE);
                allocation.setHoldExpiresAt(null);
            }
            spaceAllocationPortOut.save(allocation);
            recomputeSpace(allocation.getParkingSpaceId());
        }
    }

    public void release(UUID subscriptionId, boolean cancelled) {
        capacityAllocationPortOut.findLiveBySubscriptionId(subscriptionId).ifPresent(allocation -> {
            allocation.setAllocationStatus(cancelled
                    ? SubscriptionCapacityAllocationStatus.CANCELLED
                    : SubscriptionCapacityAllocationStatus.RELEASED);
            allocation.setHoldExpiresAt(null);
            capacityAllocationPortOut.save(allocation);
        });
        for (ParkingSpaceAllocation allocation : spaceAllocationPortOut.findActiveAllocationsBySubscription(subscriptionId)) {
            allocation.setAllocationStatus(cancelled
                    ? ParkingSpaceAllocationStatus.CANCELLED
                    : ParkingSpaceAllocationStatus.RELEASED);
            allocation.setHoldExpiresAt(null);
            spaceAllocationPortOut.save(allocation);
            recomputeSpace(allocation.getParkingSpaceId());
        }
    }

    @Transactional
    public int expireCapacityHolds(Instant now) {
        List<SubscriptionCapacityAllocation> expired = capacityAllocationPortOut.findExpiredHolds(now);
        expired.forEach(allocation -> {
            allocation.setAllocationStatus(SubscriptionCapacityAllocationStatus.CANCELLED);
            allocation.setHoldExpiresAt(null);
            capacityAllocationPortOut.save(allocation);
        });
        return expired.size();
    }

    public void recomputeSpace(UUID parkingSpaceId) {
        ParkingSpace space = spacePortOut.findByIdForUpdate(parkingSpaceId).orElse(null);
        if (space == null) {
            return;
        }
        ParkingSpaceStatus target;
        ManualOccupancyType occupancyType = null;
        var openSession = sessionPortOut.findOpenByParkingSpaceId(parkingSpaceId);
        if (openSession.isPresent()) {
            target = ParkingSpaceStatus.OCCUPIED;
            occupancyType = openSession.get().getSubscriptionId() != null
                    || openSession.get().getCustomerVehicleId() != null
                    ? ManualOccupancyType.REGISTERED : ManualOccupancyType.VISITOR;
        } else {
            boolean reserved = spaceAllocationPortOut.findByParkingSpaceId(parkingSpaceId).stream()
                    .anyMatch(this::live);
            target = reserved ? ParkingSpaceStatus.RESERVED
                    : space.getStatus() == ParkingSpaceStatus.MAINTENANCE
                            ? ParkingSpaceStatus.MAINTENANCE : ParkingSpaceStatus.AVAILABLE;
        }
        spacePolicy.setAutomaticStatus(space, target, occupancyType);
        spacePortOut.save(space);
    }

    private boolean tryHoldCapacity(Subscription subscription, UUID vehicleTypeId,
            Instant holdExpiresAt, Zone zone) {
        long effectiveCapacity = Math.max(0, value(zone.getCapacity()) - value(zone.getUnavailableCapacity()));
        long reserved = capacityAllocationPortOut.maxConcurrentReservations(zone.getZoneId(),
                subscription.getEffectiveFrom(), subscription.getEffectiveTo(), null);
        if (reserved >= effectiveCapacity) {
            return false;
        }
        SubscriptionCapacityAllocation allocation = new SubscriptionCapacityAllocation();
        allocation.setAllocationId(UUID.randomUUID());
        allocation.setSubscriptionId(subscription.getSubscriptionId());
        allocation.setZoneId(zone.getZoneId());
        allocation.setVehicleTypeId(vehicleTypeId);
        allocation.setEffectiveFrom(subscription.getEffectiveFrom());
        allocation.setEffectiveTo(subscription.getEffectiveTo());
        allocation.setAllocationStatus(SubscriptionCapacityAllocationStatus.HELD);
        allocation.setHoldExpiresAt(holdExpiresAt);
        try {
            capacityAllocationPortOut.save(allocation);
            return true;
        } catch (DataIntegrityViolationException race) {
            throw new ConflictException("Subscription capacity was reserved concurrently");
        }
    }

    private boolean tryHoldSpace(Subscription subscription, UUID vehicleTypeId,
            Instant holdExpiresAt, Zone zone) {
        List<ParkingSpace> spaces = spacePortOut.findByZoneIdAndLifecycleStatus(zone.getZoneId(), "ACTIVE")
                .stream().filter(space -> space.getVehicleTypeId() == null
                        || space.getVehicleTypeId().equals(vehicleTypeId))
                .filter(space -> !isUnavailableByStatus(space))
                .sorted(Comparator.comparing(ParkingSpace::getCode, Comparator.nullsLast(String::compareTo)))
                .toList();
        for (ParkingSpace candidate : spaces) {
            ParkingSpace space = spacePortOut.findByIdForUpdate(candidate.getParkingSpaceId()).orElse(null);
            if (space == null || sessionPortOut.existsOpenByParkingSpaceId(space.getParkingSpaceId())
                    || spaceAllocationPortOut.existsOverlappingAllocation(space.getParkingSpaceId(),
                            subscription.getEffectiveFrom(), subscription.getEffectiveTo(), null)) {
                continue;
            }
            ParkingSpaceAllocation allocation = new ParkingSpaceAllocation();
            allocation.setAllocationId(UUID.randomUUID());
            allocation.setParkingSpaceId(space.getParkingSpaceId());
            allocation.setSubscriptionId(subscription.getSubscriptionId());
            allocation.setEffectiveFrom(subscription.getEffectiveFrom());
            allocation.setEffectiveTo(subscription.getEffectiveTo());
            allocation.setHoldExpiresAt(holdExpiresAt);
            allocation.setAllocationStatus(ParkingSpaceAllocationStatus.HELD);
            try {
                spaceAllocationPortOut.save(allocation);
                spacePolicy.setAutomaticStatus(space, ParkingSpaceStatus.RESERVED, null);
                spacePortOut.save(space);
                return true;
            } catch (DataIntegrityViolationException race) {
                // The exclusion constraint is the final arbiter; inspect another candidate.
            }
        }
        return false;
    }

    private boolean live(ParkingSpaceAllocation allocation) {
        if (allocation.getAllocationStatus() == ParkingSpaceAllocationStatus.ACTIVE) {
            return true;
        }
        return allocation.getAllocationStatus() == ParkingSpaceAllocationStatus.HELD
                && allocation.getHoldExpiresAt() != null
                && allocation.getHoldExpiresAt().isAfter(Instant.now());
    }

    private ParkingSpaceAllocation requireSpaceAllocation(UUID subscriptionId) {
        return spaceAllocationPortOut.findActiveAllocationsBySubscription(subscriptionId).stream()
                .findFirst()
                .orElseThrow(() -> new ConflictException("Subscription does not use a selectable parking space"));
    }

    private ParkingSpaceAllocation requireHeldSpaceAllocation(UUID subscriptionId) {
        ParkingSpaceAllocation allocation = requireSpaceAllocation(subscriptionId);
        if (allocation.getAllocationStatus() != ParkingSpaceAllocationStatus.HELD) {
            throw new ConflictException("Subscription parking space is already finalized");
        }
        return allocation;
    }

    private SubscriptionSpaceOption toOption(Subscription subscription,
            ParkingSpaceAllocation allocation, ParkingSpace space) {
        boolean selected = allocation.getParkingSpaceId().equals(space.getParkingSpaceId());
        boolean selectable = selected || (!isUnavailableByStatus(space)
                && !sessionPortOut.existsOpenByParkingSpaceId(space.getParkingSpaceId())
                && !spaceAllocationPortOut.existsOverlappingAllocation(space.getParkingSpaceId(),
                        subscription.getEffectiveFrom(), subscription.getEffectiveTo(), allocation.getAllocationId()));
        return new SubscriptionSpaceOption(space.getParkingSpaceId(), space.getCode(), space.getStatus(),
                space.getX(), space.getY(), space.getWidth(), space.getHeight(), space.getRotation(),
                selected, selectable);
    }

    private int supportedVehicleTypeCount(Zone zone) {
        int count = zone.getVehicleTypeIds() == null ? 0 : zone.getVehicleTypeIds().size();
        if (count == 0 && zone.getVehicleTypeId() != null) {
            return 1;
        }
        return count == 0 ? Integer.MAX_VALUE : count;
    }

    private boolean isUnavailableByStatus(ParkingSpace space) {
        return space.getStatus() == ParkingSpaceStatus.MAINTENANCE
                || space.getStatus() == ParkingSpaceStatus.OCCUPIED;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean accepts(Zone zone, UUID vehicleTypeId) {
        return zone.getVehicleTypeIds() != null && zone.getVehicleTypeIds().contains(vehicleTypeId)
                || vehicleTypeId.equals(zone.getVehicleTypeId());
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }
}
