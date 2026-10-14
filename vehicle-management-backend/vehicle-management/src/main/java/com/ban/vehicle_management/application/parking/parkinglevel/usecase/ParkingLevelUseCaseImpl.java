package com.ban.vehicle_management.application.parking.parkinglevel.usecase;

import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglevel.port.in.ParkingLevelPortIn;
import com.ban.vehicle_management.application.parking.parkinglevel.port.out.ParkingLevelPortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.audit.auditlog.model.AuditLog;
import com.ban.vehicle_management.domain.parking.parkinglevel.model.ParkingLevel;
import com.ban.vehicle_management.domain.parking.parkinglevel.policy.ParkingLevelPolicy;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLevelStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParkingLevelUseCaseImpl implements ParkingLevelPortIn {

    private final ParkingLevelPortOut parkingLevelPortOut;
    private final ParkingLotPortOut parkingLotPortOut;
    private final OrganizationAccessGuard organizationAccessGuard;
    private final AuditLogPortOut auditLogPortOut;
    private final CurrentAccountPortIn currentAccountPortIn;
    private final ParkingLevelPolicy parkingLevelPolicy = new ParkingLevelPolicy();

    @Autowired
    public ParkingLevelUseCaseImpl(
            ParkingLevelPortOut parkingLevelPortOut,
            ParkingLotPortOut parkingLotPortOut,
            OrganizationAccessGuard organizationAccessGuard,
            AuditLogPortOut auditLogPortOut,
            CurrentAccountPortIn currentAccountPortIn
    ) {
        this.parkingLevelPortOut = parkingLevelPortOut;
        this.parkingLotPortOut = parkingLotPortOut;
        this.organizationAccessGuard = organizationAccessGuard;
        this.auditLogPortOut = auditLogPortOut;
        this.currentAccountPortIn = currentAccountPortIn;
    }

    @Override
    @Transactional
    public ParkingLevel createParkingLevel(ParkingLevel parkingLevel) {
        ensureCanConfigureParkingLot(parkingLevel.getParkingLotId());
        parkingLevelPolicy.initialize(parkingLevel);
        if (parkingLevelPortOut.existsByParkingLotIdAndCode(
                parkingLevel.getParkingLotId(), parkingLevel.getCode())) {
            throw new ConflictException("Parking level code already exists in this parking lot");
        }
        parkingLevel.setParkingLevelId(UUID.randomUUID());
        ParkingLevel saved = parkingLevelPortOut.save(parkingLevel);
        writeAudit(saved, "CREATE_PARKING_LEVEL", null, snapshot(saved));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public ParkingLevel getParkingLevelById(UUID parkingLevelId) {
        ParkingLevel parkingLevel = parkingLevelPortOut.findById(parkingLevelId)
                .orElseThrow(() -> new NotFoundException("Parking level not found"));
        ensureCanAccessParkingLot(parkingLevel.getParkingLotId());
        return parkingLevel;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingLevel> getParkingLevels(UUID parkingLotId, ParkingLevelStatus status, String keyword) {
        ensureCanAccessParkingLot(parkingLotId);
        return parkingLevelPortOut.findAll(parkingLotId, status, keyword);
    }

    @Override
    @Transactional
    public ParkingLevel updateParkingLevel(UUID parkingLevelId, ParkingLevel parkingLevel) {
        ParkingLevel existing = getParkingLevelById(parkingLevelId);
        ensureCanConfigureParkingLot(existing.getParkingLotId());
        if (parkingLevel.getParkingLotId() != null
                && !parkingLevel.getParkingLotId().equals(existing.getParkingLotId())) {
            throw new BadRequestException("Parking level cannot be moved to another parking lot");
        }
        existing.setCode(parkingLevel.getCode());
        existing.setName(parkingLevel.getName());
        existing.setDisplayOrder(parkingLevel.getDisplayOrder());
        existing.setElevation(parkingLevel.getElevation());
        existing.setFloorHeight(parkingLevel.getFloorHeight());
        existing.setCanvasWidth(parkingLevel.getCanvasWidth());
        existing.setCanvasHeight(parkingLevel.getCanvasHeight());
        parkingLevelPolicy.validateState(existing);
        if (parkingLevelPortOut.existsByParkingLotIdAndCodeAndParkingLevelIdNot(
                existing.getParkingLotId(), existing.getCode(), parkingLevelId)) {
            throw new ConflictException("Parking level code already exists in this parking lot");
        }
        Map<String, Object> oldData = snapshot(existing);
        ParkingLevel saved = parkingLevelPortOut.save(existing);
        writeAudit(saved, "UPDATE_PARKING_LEVEL", oldData, snapshot(saved));
        return saved;
    }

    @Override
    @Transactional
    public void deleteParkingLevel(UUID parkingLevelId) {
        ParkingLevel existing = getParkingLevelById(parkingLevelId);
        ensureCanConfigureParkingLot(existing.getParkingLotId());
        if (parkingLevelPortOut.countZonesByParkingLevelId(parkingLevelId) > 0) {
            throw new ConflictException("Parking level still has zones");
        }
        writeAudit(existing, "ARCHIVE_PARKING_LEVEL", snapshot(existing), Map.of("deleted", "true"));
        parkingLevelPortOut.delete(parkingLevelId);
    }

    @Override
    @Transactional
    public ParkingLevel activateParkingLevel(UUID parkingLevelId) {
        ParkingLevel parkingLevel = getParkingLevelById(parkingLevelId);
        ensureCanConfigureParkingLot(parkingLevel.getParkingLotId());
        Map<String, Object> oldData = snapshot(parkingLevel);
        parkingLevelPolicy.activate(parkingLevel);
        ParkingLevel saved = parkingLevelPortOut.save(parkingLevel);
        writeAudit(saved, "UPDATE_PARKING_LEVEL_STATUS", oldData, snapshot(saved));
        return saved;
    }

    @Override
    @Transactional
    public ParkingLevel markParkingLevelMaintenance(UUID parkingLevelId) {
        ParkingLevel parkingLevel = getParkingLevelById(parkingLevelId);
        ensureCanConfigureParkingLot(parkingLevel.getParkingLotId());
        Map<String, Object> oldData = snapshot(parkingLevel);
        parkingLevelPolicy.markMaintenance(parkingLevel);
        ParkingLevel saved = parkingLevelPortOut.save(parkingLevel);
        writeAudit(saved, "UPDATE_PARKING_LEVEL_STATUS", oldData, snapshot(saved));
        return saved;
    }

    @Override
    @Transactional
    public ParkingLevel closeParkingLevel(UUID parkingLevelId) {
        ParkingLevel parkingLevel = getParkingLevelById(parkingLevelId);
        ensureCanConfigureParkingLot(parkingLevel.getParkingLotId());
        Map<String, Object> oldData = snapshot(parkingLevel);
        parkingLevelPolicy.close(parkingLevel);
        ParkingLevel saved = parkingLevelPortOut.save(parkingLevel);
        writeAudit(saved, "UPDATE_PARKING_LEVEL_STATUS", oldData, snapshot(saved));
        return saved;
    }

    private void ensureCanAccessParkingLot(UUID parkingLotId) {
        if (parkingLotId == null) {
            throw new NotFoundException("Parking lot not found");
        }
        organizationAccessGuard.ensureCanAccessParkingLot(
                parkingLotPortOut.findById(parkingLotId)
                        .orElseThrow(() -> new NotFoundException("Parking lot not found"))
        );
    }

    private void ensureCanConfigureParkingLot(UUID parkingLotId) {
        if (parkingLotId == null) {
            throw new NotFoundException("Parking lot not found");
        }
        organizationAccessGuard.ensureCanConfigureParkingLot(
                parkingLotPortOut.findById(parkingLotId)
                        .orElseThrow(() -> new NotFoundException("Parking lot not found"))
        );
    }

    private Map<String, Object> snapshot(ParkingLevel parkingLevel) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("parkingLotId", text(parkingLevel.getParkingLotId()));
        values.put("code", text(parkingLevel.getCode()));
        values.put("name", text(parkingLevel.getName()));
        values.put("status", text(parkingLevel.getStatus()));
        return values;
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private void writeAudit(ParkingLevel parkingLevel, String action,
            Map<String, Object> oldData, Map<String, Object> newData) {
        if (auditLogPortOut == null || currentAccountPortIn == null) {
            return;
        }
        AuditLog auditLog = new AuditLog();
        auditLog.setAuditLogId(UUID.randomUUID());
        auditLog.setActorAccountId(currentAccountPortIn.getCurrentAccountIdOrThrow());
        auditLog.setAction(action);
        auditLog.setTargetSchema("parking");
        auditLog.setTargetTable("parking_levels");
        auditLog.setTargetId(parkingLevel.getParkingLevelId());
        auditLog.setOldData(oldData);
        auditLog.setNewData(newData);
        auditLogPortOut.save(auditLog);
    }
}
