package com.ban.vehicle_management.application.parking.parkinglayout.port.in;

import com.ban.vehicle_management.application.parking.parkinglayout.model.command.SaveLayoutElementCommand;
import com.ban.vehicle_management.application.parking.parkinglayout.model.command.SaveLayoutItemCommand;
import com.ban.vehicle_management.application.parking.parkinglayout.model.result.PublishPreviewResult;
import com.ban.vehicle_management.application.parking.parkinglayout.model.result.ZoneLayoutResult;
import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingLayoutVersion;
import java.util.List;
import java.util.UUID;

public interface ParkingLayoutPortIn {

    ZoneLayoutResult createDraftLayout(UUID zoneId);

    ZoneLayoutResult getDraftLayout(UUID zoneId);

    ZoneLayoutResult getPublishedLayout(UUID zoneId);

    List<ParkingLayoutVersion> getLayoutVersions(UUID zoneId);

    ZoneLayoutResult saveLayout(UUID zoneId, Long expectedDraftRevision,
            List<SaveLayoutItemCommand> items, List<SaveLayoutElementCommand> elements);

    ZoneLayoutResult publishLayout(UUID zoneId, Long expectedDraftRevision);

    PublishPreviewResult previewPublish(UUID zoneId);

    ParkingLayoutVersion archiveLayout(UUID zoneId, Long version);
}
