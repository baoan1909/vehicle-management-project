package com.ban.vehicle_management.application.parking.operation.usecase;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.parking.parkinglevel.port.in.ParkingLevelPortIn;
import com.ban.vehicle_management.application.parking.zone.port.in.ZonePortIn;
import com.ban.vehicle_management.application.parking.lane.port.in.LanePortIn;
import com.ban.vehicle_management.application.parking.parkingspace.port.in.ParkingSpacePortIn;
import com.ban.vehicle_management.application.parking.parkingsummary.port.in.SpaceSummaryPortIn;
import com.ban.vehicle_management.application.parking.parkinglayout.port.in.ParkingLayoutPortIn;
import com.ban.vehicle_management.application.parking.parkingsummary.model.result.LevelMapResult;
import com.ban.vehicle_management.application.parking.parkingsummary.model.result.SpaceSummaryResult;
import com.ban.vehicle_management.application.parking.parkinglayout.model.result.ZoneLayoutResult;
import com.ban.vehicle_management.domain.parking.parkinglevel.model.ParkingLevel;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.domain.parking.lane.model.Lane;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import com.ban.vehicle_management.shared.enumeration.parking.LaneStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import java.util.List;
import java.util.UUID;
import com.ban.vehicle_management.application.parking.operation.port.in.ParkingOperationReadPortIn;
import com.ban.vehicle_management.application.parking.parkingspaceallocation.port.out.ParkingSpaceAllocationPortOut;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

/** Read-only operational data. Delegated read use cases enforce the resource's lot scope.
 * Reading does not open a shift; operational writes retain their own shift checks. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParkingOperationReadUseCaseImpl implements ParkingOperationReadPortIn {
    private final CurrentAccountPortIn currentAccountPortIn;
    private final ParkingSpaceAllocationPortOut allocationPortOut;
    private final ParkingLevelPortIn parkingLevelPortIn;
    private final ZonePortIn zonePortIn;
    private final LanePortIn lanePortIn;
    private final ParkingSpacePortIn parkingSpacePortIn;
    private final SpaceSummaryPortIn spaceSummaryPortIn;
    private final ParkingLayoutPortIn parkingLayoutPortIn;

    private void requireRead() {
        var permissions = currentAccountPortIn.getCurrentAccountOrThrow().getEffectivePermissionCodes();
        if (!permissions.contains("PARKING_SPACE_READ_ALL")
                && !permissions.contains("PARKING_SESSION_CHECK_IN_ALL")
                && !permissions.contains("PARKING_SESSION_CHECK_OUT_ALL")) {
            throw new AccessDeniedException("Current account cannot read parking operations");
        }
    }

    @Override
    public List<ParkingLevel> getLevels(UUID lotId) {
        requireRead();
        return parkingLevelPortIn.getParkingLevels(lotId, null, null);
    }

    @Override
    public List<Zone> getZones(UUID lotId, ZoneStatus status) {
        requireRead();
        return zonePortIn.getZones(lotId, null, status, null);
    }

    @Override
    public List<Lane> getLanes(UUID lotId, LaneDirection direction) {
        requireRead();
        return lanePortIn.getLanes(null, lotId, direction, LaneStatus.ACTIVE, null);
    }

    @Override
    public List<ParkingSpace> getSpaces(UUID zoneId) {
        requireRead();
        return parkingSpacePortIn.getParkingSpaces(zoneId, null, "ACTIVE");
    }

    @Override
    public LevelMapResult getLevelMap(UUID levelId) {
        requireRead();
        return spaceSummaryPortIn.getLevelMap(levelId);
    }

    @Override
    public SpaceSummaryResult getLotSummary(UUID lotId) {
        requireRead();
        return spaceSummaryPortIn.getLotSummary(lotId);
    }

    @Override
    public ZoneLayoutResult getPublishedLayout(UUID zoneId) {
        requireRead();
        return parkingLayoutPortIn.getPublishedLayout(zoneId);
    }
    @Override
    public List<UUID> getAllocatedSpaceIds(UUID zoneId) {
        // getSpaces checks permission and the zone's lot scope before the allocation query.
        var spaceIds = getSpaces(zoneId).stream().map(ParkingSpace::getParkingSpaceId).toList();
        if (spaceIds.isEmpty()) return List.of();
        return allocationPortOut.findActiveAllocationsForSpacesOnDate(
                spaceIds, DateTimeUtils.toAppLocalDate(Instant.now())).stream()
                .map(allocation -> allocation.getParkingSpaceId()).distinct().toList();
    }
}

