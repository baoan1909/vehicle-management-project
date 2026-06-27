package com.ban.vehicle_management.application.operations.shift.port.out;

import com.ban.vehicle_management.domain.operations.shift.model.Shift;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShiftPortOut {

    Shift save(Shift shift);

    List<Shift> saveAll(List<Shift> shifts);

    Optional<Shift> findById(UUID shiftId);

    Optional<Shift> findByIdForUpdate(UUID shiftId);

    List<Shift> findAll(
            UUID parkingLotId,
            LocalDate fromDate,
            LocalDate toDate,
            ShiftType shiftType,
            ShiftStatus status,
            UUID employeeId,
            String keyword
    );

    List<Shift> findByParkingLotAndDateRange(
            UUID parkingLotId,
            LocalDate fromDate,
            LocalDate toDate
    );

    boolean existsByShiftCode(String shiftCode);

    boolean existsInDateRange(
            UUID parkingLotId,
            LocalDate fromDate,
            LocalDate toDate
    );

    boolean hasOpenShift(UUID parkingLotId);

    List<Shift> findByParkingLotAndDateRangeForUpdate(
            UUID parkingLotId,
            LocalDate fromDate,
            LocalDate toDate
    );
}