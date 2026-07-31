package com.ban.vehicle_management.application.parking.lane.usecase;

import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.model.NotificationAudience;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.application.parking.lane.port.in.LanePortIn;
import com.ban.vehicle_management.application.parking.lane.port.out.LanePortOut;
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

@Service
public class LaneUsecaseImpl implements LanePortIn {
    private final LanePortOut lanePortOut;
    private final NotificationPortIn notificationPortIn;
    private final LanePolicy lanePolicy = new LanePolicy();

    public  LaneUsecaseImpl(
            LanePortOut lanePortOut,
            NotificationPortIn notificationPortIn
    ){
        this.lanePortOut = lanePortOut;
        this.notificationPortIn = notificationPortIn;
    }

    @Override
    @Transactional
    public Lane createLane(Lane lane){
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
        return lanePortOut.findById(laneId).orElseThrow(() -> new NotFoundException("Lane not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lane> getLanes(UUID gateId, LaneDirection direction, LaneStatus status, String keyword){
        return lanePortOut.findAll(gateId, direction, status,normalizeKeyword(keyword));
    }

    @Override
    @Transactional
    public Lane updateLane(UUID laneId, Lane lane){
        Lane existingLane = getLaneById(laneId);

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
        validateOperationalGate(existingLane.getGateId());
        lanePolicy.activate(existingLane);
        return  lanePortOut.save(existingLane);
    }

    @Override
    @Transactional
    public Lane markLaneMaintenance(UUID laneId){
        Lane existingLane = getLaneById(laneId);
        ensureCanDisableActiveOutLane(existingLane);
        lanePolicy.markMaintenance(existingLane);
        Lane savedLane = lanePortOut.save(existingLane);
        notifyLaneMaintenance(savedLane);
        return savedLane;
    }

    @Override
    @Transactional
    public Lane forceLaneMaintenance(UUID laneId){
        Lane existingLane = getLaneById(laneId);
        lanePolicy.markMaintenance(existingLane);
        Lane savedLane = lanePortOut.save(existingLane);
        notifyLaneMaintenance(savedLane);
        return savedLane;
    }

    @Override
    @Transactional
    public  Lane closeLane(UUID laneId){
        Lane existingLane = getLaneById(laneId);

        if (existingLane.getStatus() == LaneStatus.CLOSED){
            return existingLane;
        }
        ensureCanDisableActiveOutLane(existingLane);
        lanePolicy.close(existingLane);
        return lanePortOut.save(existingLane);
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

    private void notifyLaneMaintenance(Lane lane) {
        if (notificationPortIn == null) {
            return;
        }
        notificationPortIn.sendBroadcastWebNotification(new BroadcastNotificationCommand(
                false,
                NotificationAudience.OPERATIONS,
                null,
                null,
                NotificationType.LANE_MAINTENANCE,
                "Lane bảo trì",
                "Lane " + lane.getName() + " đã chuyển sang trạng thái bảo trì.",
                null,
                "parking",
                "lanes",
                lane.getLaneId()
        ));
    }
}
