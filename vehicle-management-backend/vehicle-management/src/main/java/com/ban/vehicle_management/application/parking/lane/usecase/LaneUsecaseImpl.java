package com.ban.vehicle_management.application.parking.lane.usecase;

import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.lane.port.in.LanePortIn;
import com.ban.vehicle_management.application.parking.lane.port.out.LanePortOut;
import com.ban.vehicle_management.application.parking.gate.port.out.GatePortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.parking.lane.model.Lane;
import com.ban.vehicle_management.domain.parking.lane.policy.LanePolicy;
import com.ban.vehicle_management.domain.audit.auditlog.model.AuditLog;
import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import com.ban.vehicle_management.shared.enumeration.parking.LaneStatus;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.transaction.TransactionalEvents;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class LaneUsecaseImpl implements LanePortIn {
    private final LanePortOut lanePortOut;
    private final GatePortOut gatePortOut;
    private final ZonePortOut zonePortOut;
    private final ParkingLotPortOut parkingLotPortOut;
    private final OrganizationAccessGuard organizationAccessGuard;
    private final AuditLogPortOut auditLogPortOut;
    private final CurrentAccountPortIn currentAccountPortIn;
    private final LaneStatusNotificationDispatcher notificationDispatcher;
    private final LanePolicy lanePolicy = new LanePolicy();

    @Autowired
    public  LaneUsecaseImpl(
            LanePortOut lanePortOut,
            GatePortOut gatePortOut,
            ZonePortOut zonePortOut,
            ParkingLotPortOut parkingLotPortOut,
            OrganizationAccessGuard organizationAccessGuard,
            AuditLogPortOut auditLogPortOut,
            CurrentAccountPortIn currentAccountPortIn,
            LaneStatusNotificationDispatcher notificationDispatcher
    ){
        this.lanePortOut = lanePortOut;
        this.gatePortOut = gatePortOut;
        this.zonePortOut = zonePortOut;
        this.parkingLotPortOut = parkingLotPortOut;
        this.organizationAccessGuard = organizationAccessGuard;
        this.auditLogPortOut = auditLogPortOut;
        this.currentAccountPortIn = currentAccountPortIn;
        this.notificationDispatcher = notificationDispatcher;
    }

    public LaneUsecaseImpl(
            LanePortOut lanePortOut,
            NotificationPortIn notificationPortIn,
            GatePortOut gatePortOut,
            ZonePortOut zonePortOut,
            ParkingLotPortOut parkingLotPortOut,
            OrganizationAccessGuard organizationAccessGuard
    ) {
        this(lanePortOut, gatePortOut, zonePortOut, parkingLotPortOut, organizationAccessGuard, null, null, null);
    }

    // Kept for focused legacy unit tests that exercise only the domain policy.
    public LaneUsecaseImpl(
            LanePortOut lanePortOut,
            NotificationPortIn notificationPortIn
    ) {
        this(lanePortOut, null, null, null, null, null, null, null);
    }

    @Override
    @Transactional
    public Lane createLane(Lane lane){
        ensureCanConfigureGate(lane.getGateId());
        lanePolicy.initialize(lane);
        validateOperationalGate(lane.getGateId());

        if (lanePortOut.existsByGateIdAndCode(lane.getGateId(), lane.getCode())) {
            throw new ConflictException("Lane code already exists in this gate");
        }

        lane.setLaneId(UUID.randomUUID());
        return lanePortOut.save(lane);
    }

    @Override
    @Transactional(readOnly = true)
    public Lane getLaneById(UUID laneId){
        return findExistingLane(laneId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lane> getLanes(
            UUID gateId,
            UUID parkingLotId,
            LaneDirection direction,
            LaneStatus status,
            String keyword
    ) {
        if (gateId != null) {
            ensureCanAccessGate(gateId);
        }

        Set<UUID> scopedParkingLotIds = resolveLaneParkingLotScope(parkingLotId);
        String normalizedKeyword = normalizeKeyword(keyword);
        if (scopedParkingLotIds == null) {
            return lanePortOut.findAll(gateId, direction, status, normalizedKeyword);
        }
        return lanePortOut.findAll(gateId, direction, status, normalizedKeyword, scopedParkingLotIds);
    }

    @Override
    @Transactional
    public Lane updateLane(UUID laneId, Lane lane){
        Lane existingLane = getLaneById(laneId);
        ensureCanConfigureGate(existingLane.getGateId());

        if (isDisablingActiveOutLane(existingLane, lane.getDirection())){
            ensureCanDisableActiveOutLane(existingLane);
        }

        existingLane.setCode(lane.getCode());
        existingLane.setName(lane.getName());
        existingLane.setDirection(lane.getDirection());

        lanePolicy.initialize(existingLane);
        if (lanePortOut.existsByGateIdAndCodeAndLaneIdNot(existingLane.getGateId(), existingLane.getCode(), laneId)){
            throw new ConflictException("Lane code already exists in this gate");
        }
        return lanePortOut.save(existingLane);
    }

    @Override
    @Transactional
    public void deleteLane(UUID laneId){
        Lane existingLane = getLaneById(laneId);
        ensureCanConfigureGate(existingLane.getGateId());
        if (existingLane.getStatus() == LaneStatus.CLOSED){
            return;
        }

        ensureCanDisableActiveOutLane(existingLane);
        lanePolicy.close(existingLane);
        lanePortOut.save(existingLane);
    }

    @Override
    @Transactional
    public Lane activateLane(UUID laneId){
        Lane existingLane = getLaneById(laneId);
        UUID parkingLotId = ensureCanConfigureGate(existingLane.getGateId());
        if (existingLane.getStatus() == LaneStatus.ACTIVE) {
            return existingLane;
        }
        LaneStatus previousStatus = existingLane.getStatus();
        validateOperationalGate(existingLane.getGateId());
        lanePolicy.activate(existingLane);
        Lane savedLane = lanePortOut.save(existingLane);
        UUID actorAccountId = currentActorAccountId();
        writeLaneStatusAudit(savedLane, actorAccountId, "ACTIVATE_LANE", previousStatus);
        notifyLaneStatusChangedAfterCommit(
                savedLane,
                parkingLotId,
                actorAccountId,
                NotificationType.LANE_ACTIVATED,
                "Lane kích hoạt",
                "Lane " + savedLane.getName() + " đã chuyển sang trạng thái hoạt động.",
                "Lane " + savedLane.getName() + " đã được kích hoạt thành công."
        );
        return savedLane;
    }

    @Override
    @Transactional
    public Lane markLaneMaintenance(UUID laneId){
        Lane existingLane = getLaneById(laneId);
        UUID parkingLotId = ensureCanConfigureGate(existingLane.getGateId());
        if (existingLane.getStatus() == LaneStatus.MAINTENANCE) {
            return existingLane;
        }
        LaneStatus previousStatus = existingLane.getStatus();
        ensureCanDisableActiveOutLane(existingLane);
        lanePolicy.markMaintenance(existingLane);
        Lane savedLane = lanePortOut.save(existingLane);
        UUID actorAccountId = currentActorAccountId();
        writeLaneStatusAudit(savedLane, actorAccountId, "MARK_LANE_MAINTENANCE", previousStatus);
        notifyLaneStatusChangedAfterCommit(
                savedLane,
                parkingLotId,
                actorAccountId,
                NotificationType.LANE_MAINTENANCE,
                "Lane bảo trì",
                "Lane " + savedLane.getName() + " đã chuyển sang trạng thái bảo trì.",
                "Lane " + savedLane.getName() + " đã được chuyển sang bảo trì thành công."
        );
        return savedLane;
    }

    @Override
    @Transactional
    public Lane forceLaneMaintenance(UUID laneId){
        Lane existingLane = getLaneById(laneId);
        UUID parkingLotId = ensureCanConfigureGate(existingLane.getGateId());
        if (existingLane.getStatus() == LaneStatus.MAINTENANCE) {
            return existingLane;
        }
        LaneStatus previousStatus = existingLane.getStatus();
        lanePolicy.markMaintenance(existingLane);
        Lane savedLane = lanePortOut.save(existingLane);
        UUID actorAccountId = currentActorAccountId();
        writeLaneStatusAudit(savedLane, actorAccountId, "FORCE_LANE_MAINTENANCE", previousStatus);
        notifyLaneStatusChangedAfterCommit(
                savedLane,
                parkingLotId,
                actorAccountId,
                NotificationType.LANE_MAINTENANCE,
                "Lane bảo trì",
                "Lane " + savedLane.getName() + " đã chuyển sang trạng thái bảo trì.",
                "Lane " + savedLane.getName() + " đã được chuyển sang bảo trì thành công."
        );
        return savedLane;
    }

    @Override
    @Transactional
    public  Lane closeLane(UUID laneId){
        Lane existingLane = getLaneById(laneId);
        UUID parkingLotId = ensureCanConfigureGate(existingLane.getGateId());

        if (existingLane.getStatus() == LaneStatus.CLOSED){
            return existingLane;
        }
        LaneStatus previousStatus = existingLane.getStatus();
        ensureCanDisableActiveOutLane(existingLane);
        lanePolicy.close(existingLane);
        Lane savedLane = lanePortOut.save(existingLane);
        UUID actorAccountId = currentActorAccountId();
        writeLaneStatusAudit(savedLane, actorAccountId, "CLOSE_LANE", previousStatus);
        notifyLaneStatusChangedAfterCommit(
                savedLane,
                parkingLotId,
                actorAccountId,
                NotificationType.LANE_CLOSED,
                "Lane đóng",
                "Lane " + savedLane.getName() + " đã chuyển sang trạng thái đóng.",
                "Lane " + savedLane.getName() + " đã được đóng thành công."
        );
        return savedLane;
    }

    private boolean isDisablingActiveOutLane(Lane existingLane, LaneDirection newDirection){
        return  existingLane.getStatus() == LaneStatus.ACTIVE
                && existingLane.getDirection() == LaneDirection.OUT
                && newDirection == LaneDirection.IN;
    }

    private void ensureCanDisableActiveOutLane(Lane lane){
        if (lane.getStatus() != LaneStatus.ACTIVE || lane.getDirection() != LaneDirection.OUT){
            return;
        }

        UUID zoneId = lanePortOut.findZoneIdByGateId(lane.getGateId())
                .orElseThrow(()-> new NotFoundException("Gate zone not found"));

        if (!lanePortOut.hasOpenSessionsInZone(zoneId)){
            return;
        }
        if (!lanePortOut.hasOtherActiveOutLaneInZone(zoneId, lane.getLaneId())){
            throw new ConflictException("Cannot disable the last active OUT lane while zone has open parking sessions");
        }

    }

    private void validateOperationalGate(UUID gateId){
        if (!lanePortOut.existsOperationalGateById(gateId)){
            throw new NotFoundException("Operational gate not found");
        }
    }

    private String normalizeKeyword(String keyword){
        if (keyword == null || keyword.isBlank()){
            return null;
        }
        return  keyword.trim();
    }

    private void notifyLaneStatusChangedAfterCommit(
            Lane lane,
            UUID parkingLotId,
            UUID actorAccountId,
            NotificationType notificationType,
            String title,
            String message,
            String successDescription
    ) {
        if (notificationDispatcher == null || parkingLotId == null || actorAccountId == null) {
            return;
        }
        TransactionalEvents.runAfterCommit(() -> notificationDispatcher.dispatch(
                parkingLotId,
                actorAccountId,
                lane.getLaneId(),
                successDescription,
                notificationType,
                title,
                message
        ));
    }

    private Lane findExistingLane(UUID laneId) {
        Lane lane = lanePortOut.findById(laneId)
                .orElseThrow(() -> new NotFoundException("Lane not found"));
        ensureCanAccessGate(lane.getGateId());
        return lane;
    }

    private Set<UUID> resolveLaneParkingLotScope(UUID requestedParkingLotId) {
        if (organizationAccessGuard == null || parkingLotPortOut == null) {
            return requestedParkingLotId == null ? null : Set.of(requestedParkingLotId);
        }

        Set<UUID> accessibleParkingLotIds = organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut);
        if (requestedParkingLotId == null) {
            return accessibleParkingLotIds;
        }

        if (accessibleParkingLotIds != null && !accessibleParkingLotIds.contains(requestedParkingLotId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Current account cannot access the requested parking lot"
            );
        }
        organizationAccessGuard.ensureCanAccessParkingLot(
                parkingLotPortOut.findById(requestedParkingLotId)
                        .orElseThrow(() -> new NotFoundException("Parking lot not found"))
        );
        return Set.of(requestedParkingLotId);
    }

    private void ensureCanAccessGate(UUID gateId) {
        if (organizationAccessGuard == null
                || gatePortOut == null
                || zonePortOut == null
                || parkingLotPortOut == null) {
            return;
        }
        if (gateId == null) {
            throw new NotFoundException("Gate not found");
        }
        UUID zoneId = gatePortOut.findById(gateId)
                .orElseThrow(() -> new NotFoundException("Gate not found"))
                .getZoneId();
        UUID parkingLotId = zonePortOut.findById(zoneId)
                .orElseThrow(() -> new NotFoundException("Zone not found"))
                .getParkingLotId();
        organizationAccessGuard.ensureCanAccessParkingLot(
                parkingLotPortOut.findById(parkingLotId)
                        .orElseThrow(() -> new NotFoundException("Parking lot not found"))
        );
    }

    private UUID ensureCanConfigureGate(UUID gateId) {
        if (organizationAccessGuard == null
                || gatePortOut == null
                || zonePortOut == null
                || parkingLotPortOut == null) {
            return null;
        }
        if (gateId == null) {
            throw new NotFoundException("Gate not found");
        }
        UUID zoneId = gatePortOut.findById(gateId)
                .orElseThrow(() -> new NotFoundException("Gate not found"))
                .getZoneId();
        UUID parkingLotId = zonePortOut.findById(zoneId)
                .orElseThrow(() -> new NotFoundException("Zone not found"))
                .getParkingLotId();
        organizationAccessGuard.ensureCanConfigureParkingLot(
                parkingLotPortOut.findById(parkingLotId)
                        .orElseThrow(() -> new NotFoundException("Parking lot not found"))
        );
        return parkingLotId;
    }

    private UUID currentActorAccountId() {
        if (currentAccountPortIn == null) {
            return null;
        }
        return currentAccountPortIn.getCurrentAccountIdOrThrow();
    }

    private void writeLaneStatusAudit(
            Lane lane,
            UUID actorAccountId,
            String action,
            LaneStatus previousStatus
    ) {
        if (auditLogPortOut == null || actorAccountId == null) {
            return;
        }
        AuditLog auditLog = new AuditLog();
        auditLog.setAuditLogId(UUID.randomUUID());
        auditLog.setActorAccountId(actorAccountId);
        auditLog.setAction(action);
        auditLog.setTargetSchema("parking");
        auditLog.setTargetTable("lanes");
        auditLog.setTargetId(lane.getLaneId());
        auditLog.setOldData(laneStatusSnapshot(lane, previousStatus));
        auditLog.setNewData(laneStatusSnapshot(lane, lane.getStatus()));
        auditLogPortOut.save(auditLog);
    }

    private Map<String, Object> laneStatusSnapshot(Lane lane, LaneStatus status) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("gateId", String.valueOf(lane.getGateId()));
        snapshot.put("code", lane.getCode());
        snapshot.put("name", lane.getName());
        snapshot.put("direction", String.valueOf(lane.getDirection()));
        snapshot.put("status", String.valueOf(status));
        return snapshot;
    }
}
