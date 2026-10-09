package com.ban.vehicle_management.infrastructure.persistence.database.repository.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingLayoutElementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ParkingLayoutElementRepository extends JpaRepository<ParkingLayoutElementEntity, UUID> {

    List<ParkingLayoutElementEntity> findByLayoutVersionIdOrderByDisplayOrder(UUID layoutVersionId);

    void deleteByLayoutVersionId(UUID layoutVersionId);
}