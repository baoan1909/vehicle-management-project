package com.ban.vehicle_management.infrastructure.persistence.database.repository.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingSessionPrepaymentEntity;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionPrepaymentStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParkingSessionPrepaymentRepository
        extends JpaRepository<ParkingSessionPrepaymentEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ParkingSessionPrepaymentEntity p where p.prepaymentId = :id")
    Optional<ParkingSessionPrepaymentEntity> findLocked(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ParkingSessionPrepaymentEntity p where p.invoiceId = :invoiceId")
    Optional<ParkingSessionPrepaymentEntity> findByInvoiceIdLocked(@Param("invoiceId") UUID invoiceId);

    List<ParkingSessionPrepaymentEntity> findByParkingSessionIdAndStatusOrderByCreatedAtDesc(
            UUID parkingSessionId,
            ParkingSessionPrepaymentStatus status
    );
}
