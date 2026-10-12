package com.ban.vehicle_management.application.parking.parkingspace.usecase;

import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.parking.parkingspace.port.in.ParkingSpacePortIn;
import com.ban.vehicle_management.application.parking.parkingspace.port.out.ParkingSpacePortOut;
import com.ban.vehicle_management.application.parking.parkingspaceallocation.port.out.ParkingSpaceAllocationPortOut;
import com.ban.vehicle_management.application.parking.realtime.ParkingRealtimeAfterCommit;
import com.ban.vehicle_management.application.parking.realtime.model.ParkingRealtimeEvent;
import com.ban.vehicle_management.application.parking.realtime.port.out.ParkingRealtimePublisherPortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.domain.audit.auditlog.model.AuditLog;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.domain.parking.parkingspace.policy.ParkingSpacePolicy;
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.enumeration.parking.ManualOccupancyType;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceAllocationStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;
import com.ban.vehicle_management.shared.enumeration.parking.TrackingMode;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import io.micrometer.core.instrument.Metrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParkingSpaceUseCaseImpl implements ParkingSpacePortIn {

    private final ParkingSpacePortOut parkingSpacePortOut;
    private final ZonePortOut zonePortOut;
    private final ParkingLotPortOut parkingLotPortOut;
    private final ParkingSessionPortOut parkingSessionPortOut;
    private final ParkingSpaceAllocationPortOut allocationPortOut;
    private final OrganizationAccessGuard organizationAccessGuard;
    private final AuditLogPortOut auditLogPortOut;
    private final CurrentAccountPortIn currentAccountPortIn;
    private final ParkingRealtimePublisherPortOut parkingRealtimePublisher;
    private final ParkingSpacePolicy parkingSpacePolicy = new ParkingSpacePolicy();

    @Autowired
    public ParkingSpaceUseCaseImpl(
            ParkingSpacePortOut parkingSpacePortOut,
            ZonePortOut zonePortOut,
            ParkingLotPortOut parkingLotPortOut,
            ParkingSessionPortOut parkingSessionPortOut,
            ParkingSpaceAllocationPortOut allocationPortOut,
            OrganizationAccessGuard organizationAccessGuard,
            AuditLogPortOut auditLogPortOut,
            CurrentAccountPortIn currentAccountPortIn,
            ParkingRealtimePublisherPortOut parkingRealtimePublisher
    ) {
        this.parkingSpacePortOut = parkingSpacePortOut;
        this.zonePortOut = zonePortOut;
        this.parkingLotPortOut = parkingLotPortOut;
        this.parkingSessionPortOut = parkingSessionPortOut;
        this.allocationPortOut = allocationPortOut;
        this.organizationAccessGuard = organizationAccessGuard;
        this.auditLogPortOut = auditLogPortOut;
        this.currentAccountPortIn = currentAccountPortIn;
        this.parkingRealtimePublisher = parkingRealtimePublisher;
    }

    @Override
    @Transactional
    public ParkingSpace createParkingSpace(ParkingSpace parkingSpace) {
        Zone zone = findConfigurableZone(parkingSpace.getZoneId());
        if (zone.getTrackingMode() != TrackingMode.SPACE) {
            throw new BadRequestException("Zone is not in SPACE tracking mode");
        }
        parkingSpacePolicy.initialize(parkingSpace);
        if (parkingSpacePortOut.existsByZoneIdAndCode(zone.getZoneId(), parkingSpace.getCode())) {
            throw new ConflictException("Parking space code already exists in this zone");
        }
        parkingSpace.setParkingSpaceId(UUID.randomUUID());
        parkingSpace.setZoneId(zone.getZoneId());
        ParkingSpace saved = parkingSpacePortOut.save(parkingSpace);
        writeAudit(saved, "CREATE_PARKING_SPACE", null, snapshot(saved));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public ParkingSpace getParkingSpaceById(UUID parkingSpaceId) {
        ParkingSpace parkingSpace = getParkingSpaceOrThrow(parkingSpaceId);
        ensureCanAccessZone(parkingSpace.getZoneId());
        return parkingSpace;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSpace> getParkingSpaces(UUID zoneId, ParkingSpaceStatus status, String lifecycleStatus) {
        Zone zone = findReadableZone(zoneId);
        String lifecycle = lifecycleStatus != null ? lifecycleStatus : "ACTIVE";
        List<ParkingSpace> spaces = parkingSpacePortOut.findByZoneIdAndLifecycleStatus(zone.getZoneId(), lifecycle);
        if (status == null) {
            return spaces;
        }
        return spaces.stream().filter(space -> space.getStatus() == status).toList();
    }

    @Override
    @Transactional
    public ParkingSpace updateParkingSpace(UUID parkingSpaceId, ParkingSpace parkingSpace) {
        ParkingSpace existing = getParkingSpaceOrThrow(parkingSpaceId);
        Zone zone = findConfigurableZone(existing.getZoneId());
        if (parkingSpace.getZoneId() != null && !parkingSpace.getZoneId().equals(existing.getZoneId())) {
            throw new BadRequestException("Parking space cannot be moved to another zone");
        }
        existing.setCode(parkingSpace.getCode());
        existing.setVehicleTypeId(parkingSpace.getVehicleTypeId());
        existing.setX(parkingSpace.getX());
        existing.setY(parkingSpace.getY());
        existing.setWidth(parkingSpace.getWidth());
        existing.setHeight(parkingSpace.getHeight());
        existing.setRotation(parkingSpace.getRotation());
        parkingSpacePolicy.validateState(existing);
        if (parkingSpacePortOut.existsByZoneIdAndCodeAndParkingSpaceIdNot(
                zone.getZoneId(), existing.getCode(), parkingSpaceId)) {
            throw new ConflictException("Parking space code already exists in this zone");
        }
        Map<String, Object> oldData = snapshot(existing);
        ParkingSpace saved = parkingSpacePortOut.save(existing);
        writeAudit(saved, "UPDATE_PARKING_SPACE", oldData, snapshot(saved));
        return saved;
    }

    @Override
    @Transactional
    public void deleteParkingSpace(UUID parkingSpaceId) {
        archiveParkingSpace(parkingSpaceId);
    }

    @Override
    @Transactional
    public List<ParkingSpace> bulkCreateParkingSpaces(List<ParkingSpace> parkingSpaces) {
        if (parkingSpaces == null || parkingSpaces.isEmpty()) {
            throw new BadRequestException("No parking spaces to create");
        }
        Zone zone = findConfigurableZone(parkingSpaces.get(0).getZoneId());
        if (zone.getTrackingMode() != TrackingMode.SPACE) {
            throw new BadRequestException("Zone is not in SPACE tracking mode");
        }
        for (ParkingSpace space : parkingSpaces) {
            if (space.getZoneId() == null || !space.getZoneId().equals(zone.getZoneId())) {
                throw new BadRequestException("All spaces must belong to the same zone");
            }
            parkingSpacePolicy.initialize(space);
            if (parkingSpacePortOut.existsByZoneIdAndCode(zone.getZoneId(), space.getCode())) {
                throw new ConflictException("Parking space code already exists: " + space.getCode());
            }
            space.setParkingSpaceId(UUID.randomUUID());
            space.setZoneId(zone.getZoneId());
        }
        List<ParkingSpace> saved = parkingSpacePortOut.saveAll(parkingSpaces);
        for (ParkingSpace created : saved) {
            writeAudit(created, "CREATE_PARKING_SPACE", null, snapshot(created));
        }
        return saved;
    }

    @Override
    @Transactional
    public ParkingSpace occupyParkingSpace(UUID parkingSpaceId, ManualOccupancyType occupancyType, String reason) {
        return setManualStatus(parkingSpaceId, ParkingSpaceStatus.OCCUPIED, occupancyType, reason);
    }

    @Override
    @Transactional
    public ParkingSpace releaseParkingSpace(UUID parkingSpaceId) {
        ParkingSpace parkingSpace = getParkingSpaceOrThrow(parkingSpaceId);
        findConfigurableZone(parkingSpace.getZoneId());
        ensureNoOpenSession(parkingSpace.getParkingSpaceId());
        Map<String, Object> oldData = snapshot(parkingSpace);
        parkingSpacePolicy.release(parkingSpace);
        ParkingSpace saved = parkingSpacePortOut.save(parkingSpace);
        writeAudit(saved, "UPDATE_PARKING_SPACE_STATUS", oldData, snapshot(saved));
        return saved;
    }

    @Override
    @Transactional
    public ParkingSpace reserveParkingSpace(UUID parkingSpaceId) {
        ParkingSpace parkingSpace = getParkingSpaceOrThrow(parkingSpaceId);
        findConfigurableZone(parkingSpace.getZoneId());
        Map<String, Object> oldData = snapshot(parkingSpace);
        parkingSpacePolicy.reserve(parkingSpace);
        ParkingSpace saved = parkingSpacePortOut.save(parkingSpace);
        writeAudit(saved, "UPDATE_PARKING_SPACE_STATUS", oldData, snapshot(saved));
        return saved;
    }

    @Override
    @Transactional
    public ParkingSpace markParkingSpaceMaintenance(UUID parkingSpaceId) {
        ParkingSpace parkingSpace = getParkingSpaceOrThrow(parkingSpaceId);
        findConfigurableZone(parkingSpace.getZoneId());
        ensureNoOpenSession(parkingSpace.getParkingSpaceId());
        ensureNoActiveAllocation(parkingSpace.getParkingSpaceId());
        Map<String, Object> oldData = snapshot(parkingSpace);
        parkingSpacePolicy.markMaintenance(parkingSpace);
        ParkingSpace saved = parkingSpacePortOut.save(parkingSpace);
        writeAudit(saved, "UPDATE_PARKING_SPACE_STATUS", oldData, snapshot(saved));
        return saved;
    }

    @Override
    @Transactional
    public ParkingSpace setManualStatus(UUID parkingSpaceId, ParkingSpaceStatus targetStatus,
            ManualOccupancyType occupancyType, String reason) {
        ParkingSpace parkingSpace = getParkingSpaceOrThrow(parkingSpaceId);
        findConfigurableZone(parkingSpace.getZoneId());
        ensureStatusChangeAllowed(parkingSpace.getParkingSpaceId(), targetStatus);
        Map<String, Object> oldData = snapshot(parkingSpace);
        parkingSpacePolicy.setManualStatus(parkingSpace, targetStatus, occupancyType, reason);
        ParkingSpace saved = parkingSpacePortOut.save(parkingSpace);
        writeAudit(saved, "UPDATE_PARKING_SPACE_STATUS", oldData, snapshot(saved));
        publishStatusChanged(saved);
        return saved;
    }

    @Override
    @Transactional
    public ParkingSpace setAutomaticStatus(UUID parkingSpaceId, ParkingSpaceStatus targetStatus,
            ManualOccupancyType occupancyType) {
        ParkingSpace parkingSpace = getParkingSpaceOrThrow(parkingSpaceId);
        findConfigurableZone(parkingSpace.getZoneId());
        Map<String, Object> oldData = snapshot(parkingSpace);
        parkingSpacePolicy.setAutomaticStatus(parkingSpace, targetStatus, occupancyType);
        ParkingSpace saved = parkingSpacePortOut.save(parkingSpace);
        writeAudit(saved, "UPDATE_PARKING_SPACE_STATUS", oldData, snapshot(saved));
        publishStatusChanged(saved);
        return saved;
    }

    @Override
    @Transactional
    public ParkingSpace archiveParkingSpace(UUID parkingSpaceId) {        ParkingSpace parkingSpace = getParkingSpaceOrThrow(parkingSpaceId);
        findConfigurableZone(parkingSpace.getZoneId());
        ensureNoOpenSession(parkingSpace.getParkingSpaceId());
        ensureNoHeldOrActiveAllocation(parkingSpace.getParkingSpaceId());
        Map<String, Object> oldData = snapshot(parkingSpace);
        parkingSpacePolicy.archive(parkingSpace);
        ParkingSpace saved = parkingSpacePortOut.save(parkingSpace);
        writeAudit(saved, "ARCHIVE_PARKING_SPACE", oldData, snapshot(saved));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ParkingSession> findOpenSessionBySpace(UUID parkingSpaceId) {
        ParkingSpace parkingSpace = getParkingSpaceOrThrow(parkingSpaceId);
        ensureCanAccessZone(parkingSpace.getZoneId());
        if (parkingSessionPortOut == null) {
            return Optional.empty();
        }
        return parkingSessionPortOut.findOpenByParkingSpaceId(parkingSpace.getParkingSpaceId());
    }

    private Zone findReadableZone(UUID zoneId) {
        if (zoneId == null) {
            throw new NotFoundException("Zone not found");
        }
        Zone zone = zonePortOut.findById(zoneId)
                .orElseThrow(() -> new NotFoundException("Zone not found"));
        ensureCanAccessParkingLot(zone.getParkingLotId());
        return zone;
    }

    private Zone findConfigurableZone(UUID zoneId) {
        if (zoneId == null) {
            throw new NotFoundException("Zone not found");
        }
        Zone zone = zonePortOut.findById(zoneId)
                .orElseThrow(() -> new NotFoundException("Zone not found"));
        organizationAccessGuard.ensureCanConfigureParkingLot(getParkingLotOrThrow(zone.getParkingLotId()));
        return zone;
    }

    private void ensureCanAccessZone(UUID zoneId) {
        findReadableZone(zoneId);
    }

    private void ensureCanAccessParkingLot(UUID parkingLotId) {
        organizationAccessGuard.ensureCanAccessParkingLot(getParkingLotOrThrow(parkingLotId));
    }

    private com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot getParkingLotOrThrow(
            UUID parkingLotId) {
        if (parkingLotId == null) {
            throw new NotFoundException("Parking lot not found");
        }
        return parkingLotPortOut.findById(parkingLotId)
                .orElseThrow(() -> new NotFoundException("Parking lot not found"));
    }

    private ParkingSpace getParkingSpaceOrThrow(UUID parkingSpaceId) {
        return parkingSpacePortOut.findById(parkingSpaceId)
                .orElseThrow(() -> new NotFoundException("Parking space not found"));
    }

    private void ensureStatusChangeAllowed(UUID parkingSpaceId, ParkingSpaceStatus targetStatus) {
        if (targetStatus == ParkingSpaceStatus.OCCUPIED) {
            return;
        }
        ensureNoOpenSession(parkingSpaceId);
        if (targetStatus == ParkingSpaceStatus.MAINTENANCE) {
            ensureNoActiveAllocation(parkingSpaceId);
        }
    }

    private void ensureNoOpenSession(UUID parkingSpaceId) {
        if (parkingSessionPortOut == null) {
            return;
        }
        if (parkingSessionPortOut.existsOpenByParkingSpaceId(parkingSpaceId)) {
            throw new ConflictException("Parking space has an open parking session");
        }
    }

    private void ensureNoActiveAllocation(UUID parkingSpaceId) {
        if (allocationPortOut == null) {
            return;
        }
        if (!allocationPortOut.findActiveAllocationsForSpaceOnDate(
                parkingSpaceId, DateTimeUtils.toAppLocalDate(Instant.now())).isEmpty()) {
            throw new ConflictException("Parking space has an active subscription allocation");
        }
    }

    private void ensureNoHeldOrActiveAllocation(UUID parkingSpaceId) {
        if (allocationPortOut == null) {
            return;
        }
        boolean blocked = allocationPortOut.findByParkingSpaceId(parkingSpaceId).stream()
                .anyMatch(allocation ->
                        allocation.getAllocationStatus() == ParkingSpaceAllocationStatus.HELD
                                || allocation.getAllocationStatus() == ParkingSpaceAllocationStatus.ACTIVE);
        if (blocked) {
            throw new ConflictException("Parking space has a subscription allocation");
        }
    }

    private void publishStatusChanged(ParkingSpace space) {
        if (parkingRealtimePublisher == null || zonePortOut == null) {
            return;
        }
        Zone zone = zonePortOut.findById(space.getZoneId()).orElse(null);
        if (zone == null) {
            return;
        }
        Metrics.counter("parking.space.status.changed",
                "status", String.valueOf(space.getStatus())).increment();
        ParkingRealtimeAfterCommit.publishAfterCommit(parkingRealtimePublisher,
                new ParkingRealtimeEvent(
                        "PARKING_SPACE_STATUS_CHANGED",
                        zone.getParkingLotId(),
                        zone.getParkingLevelId(),
                        zone.getZoneId(),
                        space.getParkingSpaceId(),
                        space.getStatus() == null ? null : space.getStatus().name(),
                        zone.getLayoutVersion(),
                        Map.of("source", String.valueOf(space.getStatusSource()))));
    }

    private Map<String, Object> snapshot(ParkingSpace parkingSpace) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("zoneId", text(parkingSpace.getZoneId()));
        values.put("code", text(parkingSpace.getCode()));
        values.put("status", text(parkingSpace.getStatus()));
        values.put("lifecycleStatus", text(parkingSpace.getLifecycleStatus()));
        values.put("statusSource", text(parkingSpace.getStatusSource()));
        values.put("manualOccupancyType", text(parkingSpace.getManualOccupancyType()));
        values.put("vehicleTypeId", text(parkingSpace.getVehicleTypeId()));
        return values;
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private void writeAudit(ParkingSpace parkingSpace, String action,
            Map<String, Object> oldData, Map<String, Object> newData) {
        if (auditLogPortOut == null || currentAccountPortIn == null) {
            return;
        }
        AuditLog auditLog = new AuditLog();
        auditLog.setAuditLogId(UUID.randomUUID());
        auditLog.setActorAccountId(currentAccountPortIn.getCurrentAccountIdOrThrow());
        auditLog.setAction(action);
        auditLog.setTargetSchema("parking");
        auditLog.setTargetTable("parking_spaces");
        auditLog.setTargetId(parkingSpace.getParkingSpaceId());
        auditLog.setOldData(oldData);
        auditLog.setNewData(newData);
        auditLogPortOut.save(auditLog);
    }
}
