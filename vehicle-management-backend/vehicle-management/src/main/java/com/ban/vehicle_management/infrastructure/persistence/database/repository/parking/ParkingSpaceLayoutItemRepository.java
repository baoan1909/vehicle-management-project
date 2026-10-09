package com.ban.vehicle_management.infrastructure.persistence.database.repository.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingSpaceLayoutItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParkingSpaceLayoutItemRepository extends JpaRepository<ParkingSpaceLayoutItemEntity, UUID> {

    List<ParkingSpaceLayoutItemEntity> findByLayoutVersionId(UUID layoutVersionId);

    Optional<ParkingSpaceLayoutItemEntity> findByLayoutVersionIdAndParkingSpaceId(UUID layoutVersionId, UUID parkingSpaceId);

    void deleteByLayoutVersionId(UUID layoutVersionId);
}