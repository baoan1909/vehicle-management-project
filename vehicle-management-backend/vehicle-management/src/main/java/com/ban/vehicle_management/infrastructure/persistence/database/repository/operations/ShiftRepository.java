package com.ban.vehicle_management.infrastructure.persistence.database.repository.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ShiftEntity;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShiftRepository
        extends JpaRepository<ShiftEntity, UUID>,
        JpaSpecificationExecutor<ShiftEntity> {

    boolean existsByShiftCode(String shiftCode);

    boolean existsByParkingLotIdAndShiftDateBetween(
            UUID parkingLotId,
            LocalDate fromDate,
            LocalDate toDate
    );

    boolean existsByParkingLotIdAndStatus(
            UUID parkingLotId,
            ShiftStatus status
    );

    List<ShiftEntity>
    findAllByParkingLotIdAndShiftDateBetweenOrderByStartTimeAsc(
            UUID parkingLotId,
            LocalDate fromDate,
            LocalDate toDate
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT shiftItem
            FROM ShiftEntity shiftItem
            WHERE shiftItem.shiftId = :shiftId
            """)
    Optional<ShiftEntity> findByIdForUpdate(
            @Param("shiftId") UUID shiftId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT shiftItem
        FROM ShiftEntity shiftItem
        WHERE shiftItem.parkingLotId = :parkingLotId
          AND shiftItem.shiftDate BETWEEN :fromDate AND :toDate
        ORDER BY shiftItem.startTime ASC
        """)
    List<ShiftEntity> findAllByWeekForUpdate(
            @Param("parkingLotId") UUID parkingLotId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}