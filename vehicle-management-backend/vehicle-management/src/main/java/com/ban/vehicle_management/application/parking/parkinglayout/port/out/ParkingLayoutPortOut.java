package com.ban.vehicle_management.application.parking.parkinglayout.port.out;

import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingLayoutElement;
import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingLayoutVersion;
import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingSpaceLayoutItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParkingLayoutPortOut {

    ParkingLayoutVersion saveLayoutVersion(ParkingLayoutVersion layoutVersion);

    Optional<ParkingLayoutVersion> findLayoutVersionById(UUID layoutVersionId);

    Optional<ParkingLayoutVersion> findDraftByZoneId(UUID zoneId);

    Optional<ParkingLayoutVersion> findPublishedByZoneId(UUID zoneId);

    List<ParkingLayoutVersion> findAllByZoneId(UUID zoneId);

    Optional<ParkingLayoutVersion> findByZoneIdAndVersion(UUID zoneId, Long version);

    List<ParkingSpaceLayoutItem> saveLayoutItems(List<ParkingSpaceLayoutItem> items);

    List<ParkingSpaceLayoutItem> findLayoutItemsByVersionId(UUID layoutVersionId);

    void deleteLayoutItemsByVersionId(UUID layoutVersionId);

    List<ParkingLayoutElement> saveLayoutElements(List<ParkingLayoutElement> elements);

    List<ParkingLayoutElement> findLayoutElementsByVersionId(UUID layoutVersionId);

    void deleteLayoutElementsByVersionId(UUID layoutVersionId);

    boolean existsPublishedLayoutForZone(UUID zoneId);
}