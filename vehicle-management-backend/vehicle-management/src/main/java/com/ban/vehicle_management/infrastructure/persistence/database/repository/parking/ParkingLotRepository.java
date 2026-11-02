package com.ban.vehicle_management.infrastructure.persistence.database.repository.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingLotEntity;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotManagementState;

public interface ParkingLotRepository extends JpaRepository<ParkingLotEntity, UUID>, JpaSpecificationExecutor<ParkingLotEntity> {

    boolean existsByOrganizationIdAndCode(UUID organizationId, String code);

    boolean existsByOrganizationIdAndCodeAndParkingLotIdNot(
            UUID organizationId,
            String code,
            UUID parkingLotId
    );

    long countByOrganizationIdAndParkingLotIdIn(UUID organizationId, Collection<UUID> parkingLotIds);

    long countByOrganizationId(UUID organizationId);

    long countByOrganizationIdAndStatus(UUID organizationId, ParkingLotStatus status);

    boolean existsByParkingLotIdAndStatus(UUID parkingLotId, ParkingLotStatus status);

    boolean existsByParkingLotIdAndStatusIn(UUID parkingLotId, Set<ParkingLotStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select lot from ParkingLotEntity lot where lot.parkingLotId = :parkingLotId")
    Optional<ParkingLotEntity> findByIdForUpdate(@Param("parkingLotId") UUID parkingLotId);

    @Modifying
    @Query("""
        update ParkingLotEntity lot
        set lot.managementState = :state,
            lot.managementRevision = lot.managementRevision + 1
        where lot.parkingLotId = :parkingLotId
        """)
    int updateManagementState(
            @Param("parkingLotId") UUID parkingLotId,
            @Param("state") ParkingLotManagementState state);
}
