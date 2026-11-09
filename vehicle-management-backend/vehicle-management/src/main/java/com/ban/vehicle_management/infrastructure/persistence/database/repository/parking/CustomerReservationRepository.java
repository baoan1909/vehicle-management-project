package com.ban.vehicle_management.infrastructure.persistence.database.repository.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.CustomerReservationEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerReservationRepository extends JpaRepository<CustomerReservationEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CustomerReservationEntity r where r.reservationId = :id")
    Optional<CustomerReservationEntity> findLocked(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CustomerReservationEntity r where r.invoiceId = :invoiceId")
    Optional<CustomerReservationEntity> findByInvoiceIdLocked(@Param("invoiceId") UUID invoiceId);

    Optional<CustomerReservationEntity> findByParkingSessionId(UUID sessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CustomerReservationEntity r where r.qrToken = :token")
    Optional<CustomerReservationEntity> findByQrTokenLocked(@Param("token") String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CustomerReservationEntity r where r.parkingLotId = :lotId and r.licensePlate = :plate "
            + "and r.status = com.ban.vehicle_management.shared.enumeration.parking.CustomerReservationStatus.HELD "
            + "and r.checkInDeadline > :now")
    List<CustomerReservationEntity> findHeldByPlateLocked(@Param("lotId") UUID lotId,
            @Param("plate") String plate, @Param("now") Instant now);

    @Query("select r from CustomerReservationEntity r where r.licensePlate = :plate "
            + "and r.status = com.ban.vehicle_management.shared.enumeration.parking.CustomerReservationStatus.HELD "
            + "and r.checkInDeadline > :now")
    List<CustomerReservationEntity> findHeldByPlate(@Param("plate") String plate,
            @Param("now") Instant now);

    List<CustomerReservationEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    @Query("select r from CustomerReservationEntity r where "
            + "(r.status = com.ban.vehicle_management.shared.enumeration.parking.CustomerReservationStatus.PENDING_PAYMENT "
            + "and r.paymentHoldExpiresAt <= :now) or "
            + "(r.status = com.ban.vehicle_management.shared.enumeration.parking.CustomerReservationStatus.HELD "
            + "and r.checkInDeadline <= :now)")
    List<CustomerReservationEntity> findExpired(@Param("now") Instant now);

    @Query("select count(r) from CustomerReservationEntity r where r.zoneId = :zoneId and "
            + "((r.status = com.ban.vehicle_management.shared.enumeration.parking.CustomerReservationStatus.PENDING_PAYMENT "
            + "and r.paymentHoldExpiresAt > :now) or "
            + " (r.status = com.ban.vehicle_management.shared.enumeration.parking.CustomerReservationStatus.HELD "
            + "and r.checkInDeadline > :now))")
    long countActiveByZone(@Param("zoneId") UUID zoneId, @Param("now") Instant now);

    @Query("select count(r) from CustomerReservationEntity r where r.parkingSpaceId = :spaceId and "
            + "((r.status = com.ban.vehicle_management.shared.enumeration.parking.CustomerReservationStatus.PENDING_PAYMENT "
            + "and r.paymentHoldExpiresAt > :now) or "
            + " (r.status = com.ban.vehicle_management.shared.enumeration.parking.CustomerReservationStatus.HELD "
            + "and r.checkInDeadline > :now))")
    long countActiveBySpace(@Param("spaceId") UUID spaceId, @Param("now") Instant now);

    @Query("select count(r) from CustomerReservationEntity r where r.zoneId = :zoneId "
            + "and r.licensePlate = :plate and "
            + "((r.status = com.ban.vehicle_management.shared.enumeration.parking.CustomerReservationStatus.PENDING_PAYMENT "
            + "and r.paymentHoldExpiresAt > :now) or "
            + "(r.status = com.ban.vehicle_management.shared.enumeration.parking.CustomerReservationStatus.HELD "
            + "and r.checkInDeadline > :now))")
    long countActiveByPlateAndZone(@Param("zoneId") UUID zoneId,
            @Param("plate") String plate, @Param("now") Instant now);
}
