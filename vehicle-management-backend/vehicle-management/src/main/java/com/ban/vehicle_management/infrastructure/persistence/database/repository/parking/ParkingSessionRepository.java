package com.ban.vehicle_management.infrastructure.persistence.database.repository.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingSessionEntity;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParkingSessionRepository extends JpaRepository<ParkingSessionEntity, UUID>, JpaSpecificationExecutor<ParkingSessionEntity> {

    boolean existsByCardId(UUID cardId);

    boolean existsByCardIdAndStatusIn(UUID cardId, Collection<ParkingSessionStatus> statuses);

    boolean existsByCardIdAndStatus(UUID cardId, ParkingSessionStatus status);

    Optional<ParkingSessionEntity> findFirstByCardIdAndStatus(UUID cardId, ParkingSessionStatus status);

    boolean existsByVehicleTypeIdAndStatusIn(UUID vehicleTypeId, Collection<ParkingSessionStatus> statuses);

    long countByZoneIdAndStatus(UUID zoneId, ParkingSessionStatus status);

    boolean existsByZoneIdAndStatus(UUID zoneId, ParkingSessionStatus status);

    @Query("""
            SELECT parkingSession
            FROM ParkingSessionEntity parkingSession
            WHERE parkingSession.licensePlateIn = :licensePlateIn
              AND parkingSession.status = :status
            """)
    List<ParkingSessionEntity> findByLicensePlateInAndStatus(
            @Param("licensePlateIn") String licensePlateIn,
            @Param("status") ParkingSessionStatus status
    );

    @Override
    @EntityGraph(attributePaths = {
            "card",
            "card.cardType",
            "vehicleType",
            "zone",
            "zone.parkingLot",
            "parkingEvents",
            "parkingEvents.lane"
    })
    List<ParkingSessionEntity> findAll(Specification<ParkingSessionEntity> specification, Sort sort);
}


