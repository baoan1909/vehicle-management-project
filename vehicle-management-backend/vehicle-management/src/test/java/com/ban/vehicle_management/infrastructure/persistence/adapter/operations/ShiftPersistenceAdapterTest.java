package com.ban.vehicle_management.infrastructure.persistence.adapter.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.domain.operations.shift.model.Shift;
import com.ban.vehicle_management.infrastructure.mapper.operations.ShiftPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ShiftEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ShiftRepository;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ShiftPersistenceAdapterTest {

    @Mock
    private ShiftRepository repository;

    @Mock
    private ShiftPersistenceMapper mapper;

    @InjectMocks
    private ShiftPersistenceAdapter adapter;

    @Test
    void shouldSaveAndFlushShift() {
        Shift domain = new Shift();
        ShiftEntity entity = new ShiftEntity();

        when(mapper.toEntity(domain)).thenReturn(entity);
        when(repository.saveAndFlush(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        Shift result = adapter.save(domain);

        assertSame(domain, result);
        verify(repository).saveAndFlush(entity);
    }

    @Test
    void shouldSaveAllAndFlushShifts() {
        Shift first = new Shift();
        Shift second = new Shift();
        ShiftEntity firstEntity = new ShiftEntity();
        ShiftEntity secondEntity = new ShiftEntity();

        when(mapper.toEntity(first)).thenReturn(firstEntity);
        when(mapper.toEntity(second)).thenReturn(secondEntity);
        when(repository.saveAllAndFlush(List.of(firstEntity, secondEntity)))
                .thenReturn(List.of(firstEntity, secondEntity));
        when(mapper.toDomain(firstEntity)).thenReturn(first);
        when(mapper.toDomain(secondEntity)).thenReturn(second);

        List<Shift> result = adapter.saveAll(List.of(first, second));

        assertEquals(List.of(first, second), result);
    }

    @Test
    void shouldFindShiftById() {
        UUID shiftId = UUID.randomUUID();
        ShiftEntity entity = new ShiftEntity();
        Shift domain = new Shift();
        domain.setShiftId(shiftId);

        when(repository.findById(shiftId)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<Shift> result = adapter.findById(shiftId);

        assertTrue(result.isPresent());
        assertEquals(shiftId, result.get().getShiftId());
    }

    @Test
    void shouldFindShiftByIdForUpdate() {
        UUID shiftId = UUID.randomUUID();
        ShiftEntity entity = new ShiftEntity();
        Shift domain = new Shift();

        when(repository.findByIdForUpdate(shiftId))
                .thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<Shift> result = adapter.findByIdForUpdate(shiftId);

        assertTrue(result.isPresent());
        verify(repository).findByIdForUpdate(shiftId);
    }

    @Test
    void shouldReturnMappedFilteredShifts() {
        ShiftEntity entity = new ShiftEntity();
        Shift domain = new Shift();

        when(repository.findAll(any(Specification.class)))
                .thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<Shift> result = adapter.findAll(
                UUID.randomUUID(),
                LocalDate.of(2026, 7, 6),
                LocalDate.of(2026, 7, 12),
                ShiftType.MORNING,
                ShiftStatus.DRAFT,
                UUID.randomUUID(),
                "HCMUTE"
        );

        assertEquals(1, result.size());
        assertSame(domain, result.getFirst());
    }

    @Test
    void shouldFindParkingLotDateRange() {
        UUID parkingLotId = UUID.randomUUID();
        LocalDate fromDate = LocalDate.of(2026, 7, 6);
        LocalDate toDate = fromDate.plusDays(6);
        ShiftEntity entity = new ShiftEntity();
        Shift domain = new Shift();

        when(repository
                .findAllByParkingLotIdAndShiftDateBetweenOrderByStartTimeAsc(
                        parkingLotId,
                        fromDate,
                        toDate
                )).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<Shift> result = adapter.findByParkingLotAndDateRange(
                parkingLotId,
                fromDate,
                toDate
        );

        assertEquals(1, result.size());
    }

    @Test
    void shouldFindAndLockParkingLotWeek() {
        UUID parkingLotId = UUID.randomUUID();
        LocalDate fromDate = LocalDate.of(2026, 7, 6);
        LocalDate toDate = fromDate.plusDays(6);
        ShiftEntity entity = new ShiftEntity();
        Shift domain = new Shift();

        when(repository.findAllByWeekForUpdate(
                parkingLotId,
                fromDate,
                toDate
        )).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<Shift> result =
                adapter.findByParkingLotAndDateRangeForUpdate(
                        parkingLotId,
                        fromDate,
                        toDate
                );

        assertEquals(1, result.size());
        verify(repository).findAllByWeekForUpdate(
                parkingLotId,
                fromDate,
                toDate
        );
    }

    @Test
    void shouldDelegateExistenceChecks() {
        UUID parkingLotId = UUID.randomUUID();
        LocalDate fromDate = LocalDate.of(2026, 7, 6);
        LocalDate toDate = fromDate.plusDays(6);

        when(repository.existsByShiftCode("SHIFT-01"))
                .thenReturn(true);
        when(repository.existsByParkingLotIdAndShiftDateBetween(
                parkingLotId,
                fromDate,
                toDate
        )).thenReturn(true);
        when(repository.existsByParkingLotIdAndStatus(
                parkingLotId,
                ShiftStatus.OPEN
        )).thenReturn(true);

        assertTrue(adapter.existsByShiftCode("SHIFT-01"));
        assertTrue(adapter.existsInDateRange(
                parkingLotId,
                fromDate,
                toDate
        ));
        assertTrue(adapter.hasOpenShift(parkingLotId));
    }
}
