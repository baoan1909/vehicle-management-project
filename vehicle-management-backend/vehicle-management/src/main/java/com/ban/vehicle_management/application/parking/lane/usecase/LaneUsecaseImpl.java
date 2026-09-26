package com.ban.vehicle_management.application.parking.lane.usecase;

import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.model.NotificationAudience;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.lane.port.in.LanePortIn;
import com.ban.vehicle_management.application.parking.lane.port.out.LanePortOut;
import com.ban.vehicle_management.application.parking.gate.port.out.GatePortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.parking.lane.model.Lane;
import com.ban.vehicle_management.domain.parking.lane.policy.LanePolicy;
import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import com.ban.vehicle_management.shared.enumeration.parking.LaneStatus;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class LaneUsecaseImpl implements LanePortIn {
    private final LanePortOut lanePortOut;
    private final NotificationPortIn notificationPortIn;
    private final GatePortOut gatePortOut;
    private final ZonePortOut zonePortOut;
    private final ParkingLotPortOut parkingLotPortOut;
    private final OrganizationAccessGuard organizationAccessGuard;
    private final LanePolicy lanePolicy = new LanePolicy();

    @Autowired
    public  LaneUsecaseImpl(
            LanePortOut lanePortOut,
            NotificationPortIn notificationPortIn,
            GatePortOut gatePortOut,
            ZonePortOut zonePortOut,
            ParkingLotPortOut parkingLotPortOut,
            OrganizationAccessGuard organizationAccessGuard
    ){
        this.lanePortOut = lanePortOut;
        this.notificationPortIn = notificationPortIn;
        this.gatePortOut = gatePortOut;
        this.zonePortOut = zonePortOut;
        this.parkingLotPortOut = parkingLotPortOut;
        this.organizationAccessGuard = organizationAccessGuard;
    }

    // Kept for focused legacy unit tests that exercise only the domain policy.
    public LaneUsecaseImpl(
            LanePortOut lanePortOut,
            NotificationPortIn notificationPortIn
    ) {
        this(lanePortOut, notificationPortIn, null, null, null, null);
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
    public List<Lane> getLanes(UUID gateId, LaneDirection direction, LaneStatus status, String keyword){
        ensureCanListGateResources(gateId);
        return lanePortOut.findAll(gateId, direction, status,normalizeKeyword(keyword));
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
        ensureCanConfigureGate(existingLane.getGateId());
        validateOperationalGate(existingLane.getGateId());
        lanePolicy.activate(existingLane);
        Lane savedLane = lanePortOut.save(existingLane);
        notifyLaneStatusChanged(
                savedLane,
                "Lane kích hoạt",
                "Lane " + savedLane.getName() + " đã chuyển sang trạng thái hoạt động."
        );
        return savedLane;
    }

    @Override
    @Transactional
    public Lane markLaneMaintenance(UUID laneId){
        Lane existingLane = getLaneById(laneId);
        ensureCanConfigureGate(existingLane.getGateId());
        ensureCanDisableActiveOutLane(existingLane);
        lanePolicy.markMaintenance(existingLane);
        Lane savedLane = lanePortOut.save(existingLane);
        notifyLaneStatusChanged(
                savedLane,
                "Lane bảo trì",
                "Lane " + savedLane.getName() + " đã chuyển sang trạng thái bảo trì."
        );
        return savedLane;
    }

    @Override
    @Transactional
    public Lane forceLaneMaintenance(UUID laneId){
        Lane existingLane = getLaneById(laneId);
        ensureCanConfigureGate(existingLane.getGateId());
        lanePolicy.markMaintenance(existingLane);
        Lane savedLane = lanePortOut.save(existingLane);
        notifyLaneStatusChanged(
                savedLane,
                "Lane bảo trì",
                "Lane " + savedLane.getName() + " đã chuyển sang trạng thái bảo trì."
        );
        return savedLane;
    }

    @Override
    @Transactional
    public  Lane closeLane(UUID laneId){
        Lane existingLane = getLaneById(laneId);
        ensureCanConfigureGate(existingLane.getGateId());

        if (existingLane.getStatus() == LaneStatus.CLOSED){
            return existingLane;
        }
        ensureCanDisableActiveOutLane(existingLane);
        lanePolicy.close(existingLane);
        Lane savedLane = lanePortOut.save(existingLane);
        notifyLaneStatusChanged(
                savedLane,
                "Lane đóng",
                "Lane " + savedLane.getName() + " đã chuyển sang trạng thái đóng."
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

    private void notifyLaneStatusChanged(Lane lane, String title, String message) {
        if (notificationPortIn == null) {
            return;
        }
        notificationPortIn.sendBroadcastWebNotification(new BroadcastNotificationCommand(
                false,
                NotificationAudience.OPERATIONS,
                null,
                null,
                NotificationType.LANE_MAINTENANCE,
                title,
                message,
                null,
                "parking",
                "lanes",
                lane.getLaneId()
        ));
    }

    private Lane findExistingLane(UUID laneId) {
        Lane lane = lanePortOut.findById(laneId)
                .orElseThrow(() -> new NotFoundException("Lane not found"));
        ensureCanAccessGate(lane.getGateId());
        return lane;
    }

    private void ensureCanListGateResources(UUID gateId) {
        if (organizationAccessGuard == null) {
            return;
        }
        if (gateId == null && organizationAccessGuard.isCurrentParkingManager()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Parking manager must select a gate in an assigned parking lot"
            );
        }
        if (gateId != null) {
            ensureCanAccessGate(gateId);
        }
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

    private void ensureCanConfigureGate(UUID gateId) {
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
        organizationAccessGuard.ensureCanConfigureParkingLot(
                parkingLotPortOut.findById(parkingLotId)
                        .orElseThrow(() -> new NotFoundException("Parking lot not found"))
        );
    }
}
