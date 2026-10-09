package com.ban.vehicle_management.infrastructure.persistence.database.repository.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingSpaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParkingSpaceRepository extends JpaRepository<ParkingSpaceEntity, UUID> {

    List<ParkingSpaceEntity> findByZoneId(UUID zoneId);

    Optional<ParkingSpaceEntity> findByZoneIdAndCode(UUID zoneId, String code);

    List<ParkingSpaceEntity> findByZoneIdAndLifecycleStatus(UUID zoneId, String lifecycleStatus);

    List<ParkingSpaceEntity> findByZoneIdAndStatus(UUID zoneId, com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus status);

    @Query("SELECT ps FROM ParkingSpaceEntity ps WHERE ps.zoneId = :zoneId AND ps.status = 'OCCUPIED' AND ps.manualOccupancyType = 'REGISTERED'")
    List<ParkingSpaceEntity> findOccupiedByRegistered(UUID zoneId);

    @Query("SELECT ps FROM ParkingSpaceEntity ps WHERE ps.zoneId = :zoneId AND ps.status = 'OCCUPIED' AND ps.manualOccupancyType = 'VISITOR'")
    List<ParkingSpaceEntity> findOccupiedByVisitor(UUID zoneId);

    @Query("SELECT ps FROM ParkingSpaceEntity ps WHERE ps.zoneId = :zoneId AND ps.status = 'RESERVED'")
    List<ParkingSpaceEntity> findReserved(UUID zoneId);

    @Query("SELECT ps FROM ParkingSpaceEntity ps WHERE ps.zoneId = :zoneId AND ps.status = 'AVAILABLE'")
    List<ParkingSpaceEntity> findAvailable(UUID zoneId);

    @Query("SELECT ps FROM ParkingSpaceEntity ps WHERE ps.zoneId = :zoneId AND ps.status = 'MAINTENANCE'")
    List<ParkingSpaceEntity> findMaintenance(UUID zoneId);

    boolean existsByZoneIdAndCode(UUID zoneId, String code);

    boolean existsByZoneIdAndCodeAndParkingSpaceIdNot(UUID zoneId, String code, UUID parkingSpaceId);

    long countByZoneIdAndStatus(UUID zoneId, com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus status);

    long countByZoneIdAndLifecycleStatus(UUID zoneId, String lifecycleStatus);
}