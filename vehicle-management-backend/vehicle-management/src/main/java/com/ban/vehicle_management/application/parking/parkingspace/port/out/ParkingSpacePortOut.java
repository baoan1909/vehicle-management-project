package com.ban.vehicle_management.application.parking.parkingspace.port.out;

import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParkingSpacePortOut {

    ParkingSpace save(ParkingSpace parkingSpace);

    List<ParkingSpace> saveAll(List<ParkingSpace> parkingSpaces);

    Optional<ParkingSpace> findById(UUID parkingSpaceId);

    List<ParkingSpace> findByZoneId(UUID zoneId);

    List<ParkingSpace> findByZoneIdAndStatus(UUID zoneId, ParkingSpaceStatus status);

    List<ParkingSpace> findByZoneIdAndLifecycleStatus(UUID zoneId, String lifecycleStatus);

    Optional<ParkingSpace> findByZoneIdAndCode(UUID zoneId, String code);

    boolean existsByZoneIdAndCode(UUID zoneId, String code);

    boolean existsByZoneIdAndCodeAndParkingSpaceIdNot(UUID zoneId, String code, UUID parkingSpaceId);

    long countByZoneIdAndStatus(UUID zoneId, ParkingSpaceStatus status);

    long countByZoneIdAndLifecycleStatus(UUID zoneId, String lifecycleStatus);

    List<ParkingSpace> findOccupiedByRegistered(UUID zoneId);

    List<ParkingSpace> findOccupiedByVisitor(UUID zoneId);

    List<ParkingSpace> findReserved(UUID zoneId);

    List<ParkingSpace> findAvailable(UUID zoneId);

    List<ParkingSpace> findMaintenance(UUID zoneId);
}