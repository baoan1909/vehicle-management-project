package com.ban.vehicle_management.infrastructure.persistence.database.repository.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingLotEntity;
import java.util.Collection;
import java.util.UUID;

import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ParkingLotRepository extends JpaRepository<ParkingLotEntity, UUID>, JpaSpecificationExecutor<ParkingLotEntity> {

    boolean existsByOrganizationIdAndCode(UUID organizationId, String code);

    boolean existsByOrganizationIdAndCodeAndParkingLotIdNot(
            UUID organizationId,
            String code,
            UUID parkingLotId
    );

    long countByOrganizationIdAndParkingLotIdIn(UUID organizationId, Collection<UUID> parkingLotIds);

    boolean existsByParkingLotIdAndStatus(UUID parkingLotId, ParkingLotStatus status);
}
