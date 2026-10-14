package com.ban.vehicle_management.application.parking.parkingsummary.port.in;

import com.ban.vehicle_management.application.parking.parkingsummary.model.result.LevelMapResult;
import com.ban.vehicle_management.application.parking.parkingsummary.model.result.SpaceReconciliationIssue;
import com.ban.vehicle_management.application.parking.parkingsummary.model.result.SpaceSummaryResult;
import java.util.List;
import java.util.UUID;

public interface SpaceSummaryPortIn {

    SpaceSummaryResult getZoneSummary(UUID zoneId);

    SpaceSummaryResult getLevelSummary(UUID levelId);

    SpaceSummaryResult getLotSummary(UUID parkingLotId);

    LevelMapResult getLevelMap(UUID levelId);

    List<SpaceReconciliationIssue> findZoneReconciliationIssues(UUID zoneId);

    List<SpaceReconciliationIssue> findLotReconciliationIssues(UUID parkingLotId);
}
