package com.ban.vehicle_management.infrastructure.persistence.database.repository.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingLevelEntity;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLevelStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParkingLevelRepository extends JpaRepository<ParkingLevelEntity, UUID> {

    List<ParkingLevelEntity> findByParkingLotIdOrderByDisplayOrder(UUID parkingLotId);

    List<ParkingLevelEntity> findByParkingLotIdAndStatusOrderByDisplayOrder(UUID parkingLotId, ParkingLevelStatus status);

    Optional<ParkingLevelEntity> findByParkingLotIdAndCode(UUID parkingLotId, String code);

    boolean existsByParkingLotIdAndCode(UUID parkingLotId, String code);

    @Query("SELECT COUNT(pl) FROM ParkingLevelEntity pl WHERE pl.parkingLotId = :parkingLotId")
    long countByParkingLotId(UUID parkingLotId);
}