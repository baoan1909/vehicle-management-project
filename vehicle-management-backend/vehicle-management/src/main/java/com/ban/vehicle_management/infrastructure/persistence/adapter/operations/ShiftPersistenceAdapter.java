package com.ban.vehicle_management.infrastructure.persistence.adapter.operations;

import com.ban.vehicle_management.application.operations.shift.port.out.ShiftPortOut;
import com.ban.vehicle_management.domain.operations.shift.model.Shift;
import com.ban.vehicle_management.infrastructure.mapper.operations.ShiftPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ShiftRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.operations.ShiftSpecifications;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ShiftPersistenceAdapter implements ShiftPortOut {

    private final ShiftRepository repository;
    private final ShiftPersistenceMapper mapper;

    public ShiftPersistenceAdapter(
            ShiftRepository repository,
            ShiftPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Shift save(Shift shift) {
        return mapper.toDomain(
                repository.saveAndFlush(mapper.toEntity(shift))
        );
    }

    @Override
    public List<Shift> saveAll(List<Shift> shifts) {
        return repository.saveAllAndFlush(
                        shifts.stream()
                                .map(mapper::toEntity)
                                .toList()
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Shift> findById(UUID shiftId) {
        return repository.findById(shiftId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Shift> findByIdForUpdate(UUID shiftId) {
        return repository.findByIdForUpdate(shiftId)
                .map(mapper::toDomain);
    }

    @Override
    public List<Shift> findAll(
            UUID parkingLotId,
            LocalDate fromDate,
            LocalDate toDate,
            ShiftType shiftType,
            ShiftStatus status,
            UUID employeeId,
            String keyword
    ) {
        return repository.findAll(
                        ShiftSpecifications.withFilters(
                                parkingLotId,
                                fromDate,
                                toDate,
                                shiftType,
                                status,
                                employeeId,
                                keyword
                        )
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Shift> findByParkingLotAndDateRange(
            UUID parkingLotId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return repository
                .findAllByParkingLotIdAndShiftDateBetweenOrderByStartTimeAsc(
                        parkingLotId,
                        fromDate,
                        toDate
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByShiftCode(String shiftCode) {
        return repository.existsByShiftCode(shiftCode);
    }

    @Override
    public boolean existsInDateRange(
            UUID parkingLotId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return repository
                .existsByParkingLotIdAndShiftDateBetween(
                        parkingLotId,
                        fromDate,
                        toDate
                );
    }

    @Override
    public boolean hasOpenShift(UUID parkingLotId) {
        return repository.existsByParkingLotIdAndStatus(
                parkingLotId,
                ShiftStatus.OPEN
        );
    }

    @Override
    public List<Shift> findByParkingLotAndDateRangeForUpdate(
            UUID parkingLotId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return repository.findAllByWeekForUpdate(
                        parkingLotId,
                        fromDate,
                        toDate
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}