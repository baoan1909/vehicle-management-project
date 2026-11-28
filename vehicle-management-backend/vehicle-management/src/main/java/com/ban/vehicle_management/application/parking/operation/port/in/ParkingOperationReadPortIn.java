package com.ban.vehicle_management.application.parking.operation.port.in;
import com.ban.vehicle_management.application.parking.parkingsummary.model.result.LevelMapResult;
import com.ban.vehicle_management.application.parking.parkingsummary.model.result.SpaceSummaryResult;
import com.ban.vehicle_management.application.parking.parkinglayout.model.result.ZoneLayoutResult;
import com.ban.vehicle_management.domain.parking.parkinglevel.model.ParkingLevel;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.domain.parking.lane.model.Lane;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import java.util.List;
import java.util.UUID;
public interface ParkingOperationReadPortIn {
    List<UUID> getAllocatedSpaceIds(UUID zoneId);
    List<ParkingLevel> getLevels(UUID lotId);
    List<Zone> getZones(UUID lotId, ZoneStatus status);
    List<Lane> getLanes(UUID lotId, LaneDirection direction);
    List<ParkingSpace> getSpaces(UUID zoneId);
    LevelMapResult getLevelMap(UUID levelId);
    SpaceSummaryResult getLotSummary(UUID lotId);
    ZoneLayoutResult getPublishedLayout(UUID zoneId);
}

