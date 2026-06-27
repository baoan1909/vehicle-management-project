package com.ban.vehicle_management.infrastructure.persistence.adapter.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.domain.operations.shiftassignment.model.ShiftAssignment;
import com.ban.vehicle_management.infrastructure.mapper.operations.ShiftAssignmentPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ShiftAssignmentEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ShiftAssignmentRepository;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftAssignmentStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ShiftAssignmentPersistenceAdapterTest {

    @Mock
    private ShiftAssignmentRepository repository;

    @Mock
    private ShiftAssignmentPersistenceMapper mapper;

    @InjectMocks
    private ShiftAssignmentPersistenceAdapter adapter;

    @Test
    void shouldSaveAndFlushAssignment() {
        ShiftAssignment domain = new ShiftAssignment();
        ShiftAssignmentEntity entity = new ShiftAssignmentEntity();

        when(mapper.toEntity(domain)).thenReturn(entity);
        when(repository.saveAndFlush(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        ShiftAssignment result = adapter.save(domain);

        assertSame(domain, result);
        verify(repository).saveAndFlush(entity);
    }

    @Test
    void shouldSaveAllAndFlushAssignments() {
        ShiftAssignment first = new ShiftAssignment();
        ShiftAssignment second = new ShiftAssignment();
        ShiftAssignmentEntity firstEntity = new ShiftAssignmentEntity();
        ShiftAssignmentEntity secondEntity = new ShiftAssignmentEntity();

        when(mapper.toEntity(first)).thenReturn(firstEntity);
        when(mapper.toEntity(second)).thenReturn(secondEntity);
        when(repository.saveAllAndFlush(List.of(firstEntity, secondEntity)))
                .thenReturn(List.of(firstEntity, secondEntity));
        when(mapper.toDomain(firstEntity)).thenReturn(first);
        when(mapper.toDomain(secondEntity)).thenReturn(second);

        List<ShiftAssignment> result =
                adapter.saveAll(List.of(first, second));

        assertEquals(List.of(first, second), result);
    }

    @Test
    void shouldFindByIdAndMapDomain() {
        UUID assignmentId = UUID.randomUUID();
        ShiftAssignmentEntity entity = new ShiftAssignmentEntity();
        ShiftAssignment domain = new ShiftAssignment();
        domain.setShiftAssignmentId(assignmentId);

        when(repository.findById(assignmentId))
                .thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<ShiftAssignment> result =
                adapter.findById(assignmentId);

        assertTrue(result.isPresent());
        assertEquals(assignmentId, result.get().getShiftAssignmentId());
    }

    @Test
    void shouldLockAssignmentById() {
        UUID assignmentId = UUID.randomUUID();
        ShiftAssignmentEntity entity = new ShiftAssignmentEntity();
        ShiftAssignment domain = new ShiftAssignment();

        when(repository.findByIdForUpdate(assignmentId))
                .thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<ShiftAssignment> result =
                adapter.findByIdForUpdate(assignmentId);

        assertTrue(result.isPresent());
        verify(repository).findByIdForUpdate(assignmentId);
    }

    @Test
    void shouldLockMultipleAssignments() {
        Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID());
        ShiftAssignmentEntity entity = new ShiftAssignmentEntity();
        ShiftAssignment domain = new ShiftAssignment();

        when(repository.findAllByIdsForUpdate(ids))
                .thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<ShiftAssignment> result =
                adapter.findAllByIdsForUpdate(ids);

        assertEquals(1, result.size());
    }

    @Test
    void shouldFindAssignmentsByShiftAndStatus() {
        UUID shiftId = UUID.randomUUID();
        ShiftAssignmentEntity entity = new ShiftAssignmentEntity();
        ShiftAssignment domain = new ShiftAssignment();

        when(repository.findAllByShiftIdAndStatusOrderByCreatedAtAsc(
                shiftId,
                ShiftAssignmentStatus.ACTIVE
        )).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<ShiftAssignment> result = adapter.findByShiftId(
                shiftId,
                ShiftAssignmentStatus.ACTIVE
        );

        assertEquals(1, result.size());
    }

    @Test
    void shouldFindActiveEmployeeSchedule() {
        UUID employeeId = UUID.randomUUID();
        LocalDate fromDate = LocalDate.of(2026, 7, 6);
        LocalDate toDate = fromDate.plusDays(7);
        ShiftAssignmentEntity entity = new ShiftAssignmentEntity();
        ShiftAssignment domain = new ShiftAssignment();

        when(repository
                .findAllByEmployeeIdAndStatusAndShift_ShiftDateBetweenOrderByShift_StartTimeAsc(
                        employeeId,
                        ShiftAssignmentStatus.ACTIVE,
                        fromDate,
                        toDate
                )).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<ShiftAssignment> result =
                adapter.findActiveEmployeeSchedule(
                        employeeId,
                        fromDate,
                        toDate
                );

        assertEquals(1, result.size());
    }

    @Test
    void shouldApplyDynamicFilters() {
        ShiftAssignmentEntity entity = new ShiftAssignmentEntity();
        ShiftAssignment domain = new ShiftAssignment();

        when(repository.findAll(any(Specification.class)))
                .thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<ShiftAssignment> result = adapter.findAll(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                ShiftAssignmentStatus.ACTIVE,
                LocalDate.of(2026, 7, 6),
                LocalDate.of(2026, 7, 12),
                ShiftType.MORNING
        );

        assertEquals(1, result.size());
        assertSame(domain, result.getFirst());
    }

    @Test
    void shouldDelegateCountAndExistenceChecks() {
        UUID shiftId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID gateId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();

        when(repository.countByShiftIdAndStatus(
                shiftId,
                ShiftAssignmentStatus.ACTIVE
        )).thenReturn(2L);
        when(repository.existsByShiftIdAndEmployeeIdAndStatus(
                shiftId,
                employeeId,
                ShiftAssignmentStatus.ACTIVE
        )).thenReturn(true);
        when(repository
                .existsByShiftIdAndEmployeeIdAndStatusAndShiftAssignmentIdNot(
                        shiftId,
                        employeeId,
                        ShiftAssignmentStatus.ACTIVE,
                        assignmentId
                )).thenReturn(false);
        when(repository.existsByShiftIdAndGateIdAndStatus(
                shiftId,
                gateId,
                ShiftAssignmentStatus.ACTIVE
        )).thenReturn(true);
        when(repository
                .existsByShiftIdAndGateIdAndStatusAndShiftAssignmentIdNot(
                        shiftId,
                        gateId,
                        ShiftAssignmentStatus.ACTIVE,
                        assignmentId
                )).thenReturn(false);

        assertEquals(2L, adapter.countActiveByShiftId(shiftId));
        assertTrue(adapter.existsActiveEmployeeInShift(shiftId, employeeId));
        assertFalse(adapter.existsActiveEmployeeInShiftExcluding(
                shiftId,
                employeeId,
                assignmentId
        ));
        assertTrue(adapter.existsActiveGateInShift(shiftId, gateId));
        assertFalse(adapter.existsActiveGateInShiftExcluding(
                shiftId,
                gateId,
                assignmentId
        ));
    }

    @Test
    void shouldReturnEmptyWhenShiftIdCollectionIsEmpty() {
        List<ShiftAssignment> result = adapter.findByShiftIds(
                List.of(),
                ShiftAssignmentStatus.ACTIVE
        );

        assertTrue(result.isEmpty());
        verify(repository, never())
                .findAllByShiftIdInAndStatusOrderByCreatedAtAsc(
                        any(),
                        any()
                );
    }

    @Test
    void shouldFindAssignmentsByMultipleShiftIds() {
        List<UUID> shiftIds = List.of(
                UUID.randomUUID(),
                UUID.randomUUID()
        );
        ShiftAssignmentEntity entity = new ShiftAssignmentEntity();
        ShiftAssignment domain = new ShiftAssignment();

        when(repository.findAllByShiftIdInAndStatusOrderByCreatedAtAsc(
                shiftIds,
                ShiftAssignmentStatus.ACTIVE
        )).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<ShiftAssignment> result = adapter.findByShiftIds(
                shiftIds,
                ShiftAssignmentStatus.ACTIVE
        );

        assertEquals(1, result.size());
    }
}
