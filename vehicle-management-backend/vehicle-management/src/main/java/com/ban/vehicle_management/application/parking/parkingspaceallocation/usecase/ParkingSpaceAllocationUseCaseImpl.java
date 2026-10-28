package com.ban.vehicle_management.application.parking.parkingspaceallocation.usecase;

import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionPortOut;
import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglevel.port.out.ParkingLevelPortOut;
import com.ban.vehicle_management.application.parking.allocation.SubscriptionParkingAllocationService;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkingspace.port.out.ParkingSpacePortOut;
import com.ban.vehicle_management.application.parking.parkingspaceallocation.port.in.ParkingSpaceAllocationPortIn;
import com.ban.vehicle_management.application.parking.parkingspaceallocation.port.out.ParkingSpaceAllocationPortOut;
import com.ban.vehicle_management.application.parking.realtime.ParkingRealtimeAfterCommit;
import com.ban.vehicle_management.application.parking.realtime.model.ParkingRealtimeEvent;
import com.ban.vehicle_management.application.parking.realtime.port.out.ParkingRealtimePublisherPortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.application.people.customervehicle.port.out.CustomerVehiclePortOut;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.audit.auditlog.model.AuditLog;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.domain.parking.parkingspaceallocation.model.ParkingSpaceAllocation;
import com.ban.vehicle_management.domain.parking.parkingspaceallocation.policy.ParkingSpaceAllocationPolicy;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import io.micrometer.core.instrument.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParkingSpaceAllocationUseCaseImpl implements ParkingSpaceAllocationPortIn {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParkingSpaceAllocationUseCaseImpl.class);
    private static final String PARKING_SPACE_READ_ALL = "PARKING_SPACE_READ_ALL";
    private static final String PARKING_SPACE_ASSIGN_ALL = "PARKING_SPACE_ASSIGN_ALL";

    private final ParkingSpaceAllocationPortOut allocationPortOut;
    private final ParkingSpacePortOut parkingSpacePortOut;
    private final ZonePortOut zonePortOut;
    private final ParkingLevelPortOut parkingLevelPortOut;
    private final ParkingLotPortOut parkingLotPortOut;
    private final SubscriptionPortOut subscriptionPortOut;
    private final CustomerVehiclePortOut customerVehiclePortOut;
    private final OrganizationAccessGuard organizationAccessGuard;
    private final AuditLogPortOut auditLogPortOut;
    private final CurrentAccountPortIn currentAccountPortIn;
    private final ParkingRealtimePublisherPortOut parkingRealtimePublisher;
    private final SubscriptionParkingAllocationService allocationLifecycleService;
    private final ParkingSpaceAllocationPolicy policy = new ParkingSpaceAllocationPolicy();

    @Autowired
    public ParkingSpaceAllocationUseCaseImpl(
            ParkingSpaceAllocationPortOut allocationPortOut,
            ParkingSpacePortOut parkingSpacePortOut,
            ZonePortOut zonePortOut,
            ParkingLevelPortOut parkingLevelPortOut,
            ParkingLotPortOut parkingLotPortOut,
            SubscriptionPortOut subscriptionPortOut,
            CustomerVehiclePortOut customerVehiclePortOut,
            OrganizationAccessGuard organizationAccessGuard,
            AuditLogPortOut auditLogPortOut,
            CurrentAccountPortIn currentAccountPortIn,
            ParkingRealtimePublisherPortOut parkingRealtimePublisher,
            SubscriptionParkingAllocationService allocationLifecycleService
    ) {
        this.allocationPortOut = allocationPortOut;
        this.parkingSpacePortOut = parkingSpacePortOut;
        this.zonePortOut = zonePortOut;
        this.parkingLevelPortOut = parkingLevelPortOut;
        this.parkingLotPortOut = parkingLotPortOut;
        this.subscriptionPortOut = subscriptionPortOut;
        this.customerVehiclePortOut = customerVehiclePortOut;
        this.organizationAccessGuard = organizationAccessGuard;
        this.auditLogPortOut = auditLogPortOut;
        this.currentAccountPortIn = currentAccountPortIn;
        this.parkingRealtimePublisher = parkingRealtimePublisher;
        this.allocationLifecycleService = allocationLifecycleService;
    }

    @Override
    @Transactional
    public ParkingSpaceAllocation createAllocation(ParkingSpaceAllocation allocation) {
        currentAccountPortIn.requirePermission(PARKING_SPACE_ASSIGN_ALL);
        ParkingSpace space = ensureCanOperateSpace(allocation.getParkingSpaceId());
        space = parkingSpacePortOut.findByIdForUpdate(space.getParkingSpaceId())
                .or(() -> parkingSpacePortOut.findById(allocation.getParkingSpaceId()))
                .orElseThrow(() -> new NotFoundException("Parking space not found"));
        Subscription subscription = findSubscription(allocation.getSubscriptionId());
        validateAllocationScope(space, subscription);
        policy.initialize(allocation);
        cancelExpiredHolds(allocation.getParkingSpaceId());
        policy.validateNoOverlap(allocation,
                allocationPortOut.findByParkingSpaceId(allocation.getParkingSpaceId()));
        if (allocationPortOut.existsOverlappingAllocation(
                allocation.getParkingSpaceId(),
                allocation.getEffectiveFrom(),
                allocation.getEffectiveTo(),
                null)) {
            throw new ConflictException("Parking space already allocated for an overlapping period");
        }
        allocation.setAllocationId(UUID.randomUUID());
        ParkingSpaceAllocation saved = allocationPortOut.save(allocation);
        recompute(saved.getParkingSpaceId());
        writeAudit(saved, "CREATE_PARKING_SPACE_ALLOCATION", null, snapshot(saved));
        publishAllocationChanged(space, saved);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public ParkingSpaceAllocation getAllocationById(UUID allocationId) {
        currentAccountPortIn.requirePermission(PARKING_SPACE_READ_ALL);
        ParkingSpaceAllocation allocation = allocationPortOut.findById(allocationId)
                .orElseThrow(() -> new NotFoundException("Parking space allocation not found"));
        ensureCanReadSpace(allocation.getParkingSpaceId());
        return allocation;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSpaceAllocation> getAllocationsByParkingSpaceId(UUID parkingSpaceId) {
        currentAccountPortIn.requirePermission(PARKING_SPACE_READ_ALL);
        ensureCanReadSpace(parkingSpaceId);
        return allocationPortOut.findByParkingSpaceId(parkingSpaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSpaceAllocation> getAllocationsBySubscriptionId(UUID subscriptionId) {
        currentAccountPortIn.requirePermission(PARKING_SPACE_READ_ALL);
        ensureCanReadSubscription(subscriptionId);
        return allocationPortOut.findBySubscriptionId(subscriptionId);
    }

    @Override
    @Transactional
    public ParkingSpaceAllocation activateAllocation(UUID allocationId) {
        currentAccountPortIn.requirePermission(PARKING_SPACE_ASSIGN_ALL);
        ParkingSpaceAllocation allocation = findAllocationForWrite(allocationId);
        ParkingSpace space = parkingSpacePortOut.findByIdForUpdate(allocation.getParkingSpaceId())
                .or(() -> parkingSpacePortOut.findById(allocation.getParkingSpaceId()))
                .orElseThrow(() -> new NotFoundException("Parking space not found"));
        Subscription subscription = findSubscription(allocation.getSubscriptionId());
        validateAllocationScope(space, subscription);
        cancelExpiredHolds(allocation.getParkingSpaceId());
        policy.validateNoOverlap(allocation,
                allocationPortOut.findByParkingSpaceId(allocation.getParkingSpaceId()));
        if (allocationPortOut.existsOverlappingAllocation(
                allocation.getParkingSpaceId(),
                allocation.getEffectiveFrom(),
                allocation.getEffectiveTo(),
                allocation.getAllocationId())) {
            throw new ConflictException("Parking space already allocated for an overlapping period");
        }
        Map<String, Object> oldData = snapshot(allocation);
        policy.activate(allocation);
        ParkingSpaceAllocation saved = allocationPortOut.save(allocation);
        recompute(saved.getParkingSpaceId());
        writeAudit(saved, "ACTIVATE_PARKING_SPACE_ALLOCATION", oldData, snapshot(saved));
        publishAllocationChanged(space, saved);
        return saved;
    }

    @Override
    @Transactional
    public ParkingSpaceAllocation releaseAllocation(UUID allocationId) {
        currentAccountPortIn.requirePermission(PARKING_SPACE_ASSIGN_ALL);
        ParkingSpaceAllocation allocation = findAllocationForWrite(allocationId);
        Map<String, Object> oldData = snapshot(allocation);
        policy.release(allocation);
        ParkingSpaceAllocation saved = allocationPortOut.save(allocation);
        recompute(saved.getParkingSpaceId());
        writeAudit(saved, "RELEASE_PARKING_SPACE_ALLOCATION", oldData, snapshot(saved));
        publishAllocationChanged(null, saved);
        return saved;
    }

    @Override
    @Transactional
    public ParkingSpaceAllocation cancelAllocation(UUID allocationId) {
        currentAccountPortIn.requirePermission(PARKING_SPACE_ASSIGN_ALL);
        ParkingSpaceAllocation allocation = findAllocationForWrite(allocationId);
        Map<String, Object> oldData = snapshot(allocation);
        policy.cancel(allocation);
        ParkingSpaceAllocation saved = allocationPortOut.save(allocation);
        recompute(saved.getParkingSpaceId());
        writeAudit(saved, "CANCEL_PARKING_SPACE_ALLOCATION", oldData, snapshot(saved));
        publishAllocationChanged(null, saved);
        return saved;
    }

    @Override
    @Transactional
    public ParkingSpaceAllocation setHoldExpiration(UUID allocationId, Instant holdExpiresAt) {
        currentAccountPortIn.requirePermission(PARKING_SPACE_ASSIGN_ALL);
        ParkingSpaceAllocation allocation = findAllocationForWrite(allocationId);
        Map<String, Object> oldData = snapshot(allocation);
        policy.setHoldExpiration(allocation, holdExpiresAt);
        ParkingSpaceAllocation saved = allocationPortOut.save(allocation);
        writeAudit(saved, "HOLD_PARKING_SPACE_ALLOCATION", oldData, snapshot(saved));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSpaceAllocation> findActiveAllocationsForSpaceOnDate(UUID parkingSpaceId, LocalDate date) {
        currentAccountPortIn.requirePermission(PARKING_SPACE_READ_ALL);
        ensureCanReadSpace(parkingSpaceId);
        return allocationPortOut.findActiveAllocationsForSpaceOnDate(parkingSpaceId, date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSpaceAllocation> findActiveAllocationsBySubscription(UUID subscriptionId) {
        currentAccountPortIn.requirePermission(PARKING_SPACE_READ_ALL);
        ensureCanReadSubscription(subscriptionId);
        return allocationPortOut.findActiveAllocationsBySubscription(subscriptionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSpaceAllocation> findEffectiveAllocationsByZone(UUID zoneId, LocalDate date) {
        currentAccountPortIn.requirePermission(PARKING_SPACE_READ_ALL);
        if (zoneId == null) {
            throw new BadRequestException("zoneId must not be null");
        }
        var zone = zonePortOut.findById(zoneId)
                .orElseThrow(() -> new NotFoundException("Zone not found"));
        var lot = parkingLotPortOut.findById(zone.getParkingLotId())
                .orElseThrow(() -> new NotFoundException("Parking lot not found"));
        organizationAccessGuard.ensureCanAccessParkingLot(lot);
        LocalDate effectiveDate = date == null
                ? com.ban.vehicle_management.shared.utils.DateTimeUtils.toAppLocalDate(Instant.now())
                : date;
        List<UUID> spaceIds = parkingSpacePortOut.findByZoneIdAndLifecycleStatus(zoneId, "ACTIVE")
                .stream()
                .map(ParkingSpace::getParkingSpaceId)
                .toList();
        if (spaceIds.isEmpty()) {
            return List.of();
        }
        return allocationPortOut.findActiveAllocationsForSpacesOnDate(spaceIds, effectiveDate);
    }

    @Override
    @Transactional
    public int expireHeldAllocations() {
        Instant now = Instant.now();
        List<ParkingSpaceAllocation> expiredAllocations = allocationPortOut.findExpiredHolds(now);
        int expired = allocationPortOut.cancelExpiredHoldsGlobal(now);
        if (expired > 0) {
            expiredAllocations.forEach(allocation -> {
                allocation.setAllocationStatus(
                        com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceAllocationStatus.CANCELLED);
                recompute(allocation.getParkingSpaceId());
                publishAllocationChanged(null, allocation);
            });
            Metrics.counter("parking.allocation.hold.expired.scheduler").increment(expired);
            LOGGER.info("scheduler expired {} held allocation(s)", expired);
        }
        return expired;
    }

    private void cancelExpiredHolds(UUID parkingSpaceId) {
        int expired = 0;
        for (ParkingSpaceAllocation item : allocationPortOut.findExpiredHolds(parkingSpaceId, Instant.now())) {
            Map<String, Object> oldData = snapshot(item);
            policy.cancel(item);
            ParkingSpaceAllocation saved = allocationPortOut.save(item);
            writeAudit(saved, "EXPIRE_PARKING_SPACE_ALLOCATION_HOLD", oldData, snapshot(saved));
            publishAllocationChanged(null, saved);
            expired++;
        }
        if (expired > 0) {
            Metrics.counter("parking.allocation.hold.expired").increment(expired);
            LOGGER.info("expired {} held allocation(s) for space {}", expired, parkingSpaceId);
        }
    }
    private void publishAllocationChanged(ParkingSpace knownSpace, ParkingSpaceAllocation allocation) {
        UUID spaceId = allocation.getParkingSpaceId();
        ParkingSpace space = knownSpace;
        if (space == null && spaceId != null && parkingSpacePortOut != null) {
            space = parkingSpacePortOut.findById(spaceId).orElse(null);
        }
        UUID lotId = null;
        UUID zoneId = null;
        UUID levelId = null;
        if (space != null && zonePortOut != null) {
            var zone = zonePortOut.findById(space.getZoneId()).orElse(null);
            if (zone != null) {
                lotId = zone.getParkingLotId();
                zoneId = zone.getZoneId();
                levelId = zone.getParkingLevelId();
            }
        }
        if (lotId == null) {
            return;
        }
        ParkingRealtimeAfterCommit.publishAfterCommit(parkingRealtimePublisher,
                new ParkingRealtimeEvent(
                        "PARKING_SPACE_ALLOCATION_CHANGED",
                        lotId,
                        levelId,
                        zoneId,
                        spaceId,
                        allocation.getAllocationStatus() == null
                                ? null
                                : allocation.getAllocationStatus().name(),
                        null,
                        Map.of("allocationChanged", "true",
                                "reserved", String.valueOf(allocation.getAllocationStatus()
                                        == com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceAllocationStatus.HELD
                                        || allocation.getAllocationStatus()
                                        == com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceAllocationStatus.ACTIVE))));
    }

    private void recompute(UUID parkingSpaceId) {
        if (allocationLifecycleService != null) {
            allocationLifecycleService.recomputeSpace(parkingSpaceId);
        }
    }

    private ParkingSpaceAllocation findAllocationForWrite(UUID allocationId) {
        ParkingSpaceAllocation allocation = allocationPortOut.findById(allocationId)
                .orElseThrow(() -> new NotFoundException("Parking space allocation not found"));
        ensureCanOperateSpace(allocation.getParkingSpaceId());
        return allocation;
    }

    private ParkingSpace ensureCanReadSpace(UUID parkingSpaceId) {
        return ensureSpaceAccess(parkingSpaceId, false);
    }

    private ParkingSpace ensureCanOperateSpace(UUID parkingSpaceId) {
        return ensureSpaceAccess(parkingSpaceId, true);
    }

    private ParkingSpace ensureSpaceAccess(UUID parkingSpaceId, boolean write) {
        if (parkingSpaceId == null) {
            throw new BadRequestException("parkingSpaceId must not be null");
        }
        ParkingSpace space = parkingSpacePortOut.findById(parkingSpaceId)
                .orElseThrow(() -> new NotFoundException("Parking space not found"));
        var zone = zonePortOut.findById(space.getZoneId())
                .orElseThrow(() -> new NotFoundException("Zone not found"));
        if (zone.getParkingLevelId() == null) {
            throw new BadRequestException("Zone has no parking level assigned");
        }
        var level = parkingLevelPortOut.findById(zone.getParkingLevelId())
                .orElseThrow(() -> new NotFoundException("Parking level not found"));
        if (!zone.getParkingLotId().equals(level.getParkingLotId())) {
            throw new BadRequestException("Space, zone, level and lot must share the same topology");
        }
        var lot = parkingLotPortOut.findById(zone.getParkingLotId())
                .orElseThrow(() -> new NotFoundException("Parking lot not found"));
        if (write) {
            organizationAccessGuard.ensureCanOperateParkingLot(lot);
        } else {
            organizationAccessGuard.ensureCanAccessParkingLot(lot);
        }
        return space;
    }

    private Subscription findSubscription(UUID subscriptionId) {
        if (subscriptionId == null) {
            throw new BadRequestException("subscriptionId must not be null");
        }
        return subscriptionPortOut.findById(subscriptionId)
                .orElseThrow(() -> new NotFoundException("Subscription not found"));
    }

    private void ensureCanReadSubscription(UUID subscriptionId) {
        Subscription subscription = findSubscription(subscriptionId);
        var lot = parkingLotPortOut.findById(subscription.getParkingLotId())
                .orElseThrow(() -> new NotFoundException("Parking lot not found"));
        organizationAccessGuard.ensureCanAccessParkingLot(lot);
    }

    private void validateAllocationScope(ParkingSpace space, Subscription subscription) {
        if (!"ACTIVE".equals(space.getLifecycleStatus())) {
            throw new ConflictException("Only an active parking space can be allocated");
        }
        if (space.getStatus() == ParkingSpaceStatus.MAINTENANCE) {
            throw new ConflictException("A parking space in maintenance cannot be allocated");
        }
        var zone = zonePortOut.findById(space.getZoneId())
                .orElseThrow(() -> new NotFoundException("Zone not found"));
        if (!zone.getParkingLotId().equals(subscription.getParkingLotId())) {
            throw new BadRequestException("Subscription and parking space must belong to the same parking lot");
        }
        var vehicle = customerVehiclePortOut.findById(subscription.getCustomerVehicleId())
                .orElseThrow(() -> new NotFoundException("Subscription vehicle not found"));
        if (space.getVehicleTypeId() != null
                && !space.getVehicleTypeId().equals(vehicle.getVehicleTypeId())) {
            throw new ConflictException("Parking space does not support the subscription vehicle type");
        }
    }
    private Map<String, Object> snapshot(ParkingSpaceAllocation allocation) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("parkingSpaceId", text(allocation.getParkingSpaceId()));
        values.put("subscriptionId", text(allocation.getSubscriptionId()));
        values.put("effectiveFrom", text(allocation.getEffectiveFrom()));
        values.put("effectiveTo", text(allocation.getEffectiveTo()));
        values.put("allocationStatus", text(allocation.getAllocationStatus()));
        return values;
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private void writeAudit(ParkingSpaceAllocation allocation, String action,
            Map<String, Object> oldData, Map<String, Object> newData) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAuditLogId(UUID.randomUUID());
        auditLog.setActorAccountId(currentAccountPortIn.getCurrentAccountIdOrThrow());
        auditLog.setAction(action);
        auditLog.setTargetSchema("parking");
        auditLog.setTargetTable("parking_space_allocations");
        auditLog.setTargetId(allocation.getAllocationId());
        auditLog.setOldData(oldData);
        auditLog.setNewData(newData);
        auditLogPortOut.save(auditLog);
    }
}
