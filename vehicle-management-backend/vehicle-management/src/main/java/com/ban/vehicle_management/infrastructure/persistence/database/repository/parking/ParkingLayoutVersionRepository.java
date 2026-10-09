package com.ban.vehicle_management.infrastructure.persistence.database.repository.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingLayoutVersionEntity;
import com.ban.vehicle_management.shared.enumeration.parking.LayoutVersionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingLayoutVersionRepository extends JpaRepository<ParkingLayoutVersionEntity, UUID> {

    List<ParkingLayoutVersionEntity> findByZoneIdOrderByVersionDesc(UUID zoneId);

    Optional<ParkingLayoutVersionEntity> findByZoneIdAndVersion(UUID zoneId, Long version);

    Optional<ParkingLayoutVersionEntity> findFirstByZoneIdAndStatusOrderByVersionDesc(
            UUID zoneId, LayoutVersionStatus status);
}
