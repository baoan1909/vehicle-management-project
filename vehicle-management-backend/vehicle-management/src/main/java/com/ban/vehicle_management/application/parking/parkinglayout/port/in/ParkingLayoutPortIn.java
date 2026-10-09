package com.ban.vehicle_management.application.parking.parkinglayout.port.in;

import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingLayoutElement;
import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingLayoutVersion;
import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingSpaceLayoutItem;
import java.util.List;
import java.util.UUID;

public interface ParkingLayoutPortIn {

    ParkingLayoutVersion createDraftLayout(UUID zoneId);

    ParkingLayoutVersion getDraftLayout(UUID zoneId);

    ParkingLayoutVersion getPublishedLayout(UUID zoneId);

    List<ParkingLayoutVersion> getLayoutVersions(UUID zoneId);

    ParkingLayoutVersion saveLayoutGeometry(UUID zoneId, List<ParkingSpaceLayoutItem> items, List<ParkingLayoutElement> elements);

    ParkingLayoutVersion publishLayout(UUID zoneId);

    ParkingLayoutVersion archiveLayout(UUID zoneId, Long version);
}