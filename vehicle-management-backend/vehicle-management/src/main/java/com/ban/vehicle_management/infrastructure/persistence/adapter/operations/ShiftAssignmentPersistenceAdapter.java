package com.ban.vehicle_management.infrastructure.persistence.adapter.operations;

import com.ban.vehicle_management.application.operations.shiftassignment.port.out.ShiftAssignmentPortOut;
import com.ban.vehicle_management.domain.operations.shiftassignment.model.ShiftAssignment;
import com.ban.vehicle_management.infrastructure.mapper.operations.ShiftAssignmentPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ShiftAssignmentRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.operations.ShiftAssignmentSpecifications;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftAssignmentStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ShiftAssignmentPersistenceAdapter
        implements ShiftAssignmentPortOut {

    private final ShiftAssignmentRepository repository;
    private final ShiftAssignmentPersistenceMapper mapper;

    public ShiftAssignmentPersistenceAdapter(
            ShiftAssignmentRepository repository,
            ShiftAssignmentPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public ShiftAssignment save(ShiftAssignment assignment) {
        return mapper.toDomain(
                repository.saveAndFlush(mapper.toEntity(assignment))
        );
    }

    @Override
    public List<ShiftAssignment> saveAll(
            List<ShiftAssignment> assignments
    ) {
        return repository.saveAllAndFlush(
                        assignments.stream()
                                .map(mapper::toEntity)
                                .toList()
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ShiftAssignment> findById(UUID assignmentId) {
        return repository.findById(assignmentId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<ShiftAssignment> findByIdForUpdate(
            UUID assignmentId
    ) {
        return repository.findByIdForUpdate(assignmentId)
                .map(mapper::toDomain);
    }

    @Override
    public List<ShiftAssignment> findAllByIdsForUpdate(
            Collection<UUID> assignmentIds
    ) {
        return repository.findAllByIdsForUpdate(assignmentIds)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ShiftAssignment> findByShiftId(
            UUID shiftId,
            ShiftAssignmentStatus status
    ) {
        return repository
                .findAllByShiftIdAndStatusOrderByCreatedAtAsc(
                        shiftId,
                        status
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ShiftAssignment> findActiveEmployeeSchedule(
            UUID employeeId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return repository
                .findAllByEmployeeIdAndStatusAndShift_ShiftDateBetweenOrderByShift_StartTimeAsc(
                        employeeId,
                        ShiftAssignmentStatus.ACTIVE,
                        fromDate,
                        toDate
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ShiftAssignment> findAll(
            UUID parkingLotId,
            UUID shiftId,
            UUID employeeId,
            UUID gateId,
            ShiftAssignmentStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            ShiftType shiftType
    ) {
        return repository.findAll(
                        ShiftAssignmentSpecifications.withFilters(
                                parkingLotId,
                                shiftId,
                                employeeId,
                                gateId,
                                status,
                                fromDate,
                                toDate,
                                shiftType
                        )
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countActiveByShiftId(UUID shiftId) {
        return repository.countByShiftIdAndStatus(
                shiftId,
                ShiftAssignmentStatus.ACTIVE
        );
    }

    @Override
    public boolean existsActiveEmployeeInShift(
            UUID shiftId,
            UUID employeeId
    ) {
        return repository.existsByShiftIdAndEmployeeIdAndStatus(
                shiftId,
                employeeId,
                ShiftAssignmentStatus.ACTIVE
        );
    }

    @Override
    public boolean existsActiveEmployeeInShiftExcluding(
            UUID shiftId,
            UUID employeeId,
            UUID assignmentId
    ) {
        return repository
                .existsByShiftIdAndEmployeeIdAndStatusAndShiftAssignmentIdNot(
                        shiftId,
                        employeeId,
                        ShiftAssignmentStatus.ACTIVE,
                        assignmentId
                );
    }

    @Override
    public boolean existsActiveGateInShift(
            UUID shiftId,
            UUID gateId
    ) {
        return repository.existsByShiftIdAndGateIdAndStatus(
                shiftId,
                gateId,
                ShiftAssignmentStatus.ACTIVE
        );
    }

    @Override
    public boolean existsActiveGateInShiftExcluding(
            UUID shiftId,
            UUID gateId,
            UUID assignmentId
    ) {
        return repository
                .existsByShiftIdAndGateIdAndStatusAndShiftAssignmentIdNot(
                        shiftId,
                        gateId,
                        ShiftAssignmentStatus.ACTIVE,
                        assignmentId
                );
    }

    @Override
    public List<ShiftAssignment> findByShiftIds(
            Collection<UUID> shiftIds,
            ShiftAssignmentStatus status
    ) {
        if (shiftIds == null || shiftIds.isEmpty()) {
            return List.of();
        }

        return repository
                .findAllByShiftIdInAndStatusOrderByCreatedAtAsc(
                        shiftIds,
                        status
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ShiftAssignment> findNotRemovedByShiftId(UUID shiftId) {
        return repository
                .findAllByShiftIdAndStatusNotOrderByCreatedAtAsc(
                        shiftId,
                        ShiftAssignmentStatus.REMOVED
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ShiftAssignment> findNotRemovedEmployeeSchedule(
            UUID employeeId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return repository
                .findAllByEmployeeIdAndStatusNotAndShift_ShiftDateBetweenOrderByShift_StartTimeAsc(
                        employeeId,
                        ShiftAssignmentStatus.REMOVED,
                        fromDate,
                        toDate
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ShiftAssignment> findNotRemovedByShiftIds(
            Collection<UUID> shiftIds
    ) {
        if (shiftIds == null || shiftIds.isEmpty()) {
            return List.of();
        }

        return repository
                .findAllByShiftIdInAndStatusNotOrderByCreatedAtAsc(
                        shiftIds,
                        ShiftAssignmentStatus.REMOVED
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countNotRemovedByShiftId(UUID shiftId) {
        return repository.countByShiftIdAndStatusNot(
                shiftId,
                ShiftAssignmentStatus.REMOVED
        );
    }

    @Override
    public boolean existsNotRemovedEmployeeInShift(
            UUID shiftId,
            UUID employeeId
    ) {
        return repository.existsByShiftIdAndEmployeeIdAndStatusNot(
                shiftId,
                employeeId,
                ShiftAssignmentStatus.REMOVED
        );
    }

    @Override
    public boolean existsNotRemovedEmployeeInShiftExcluding(
            UUID shiftId,
            UUID employeeId,
            UUID assignmentId
    ) {
        return repository
                .existsByShiftIdAndEmployeeIdAndStatusNotAndShiftAssignmentIdNot(
                        shiftId,
                        employeeId,
                        ShiftAssignmentStatus.REMOVED,
                        assignmentId
                );
    }

    @Override
    public boolean existsNotRemovedGateInShift(
            UUID shiftId,
            UUID gateId
    ) {
        return repository.existsByShiftIdAndGateIdAndStatusNot(
                shiftId,
                gateId,
                ShiftAssignmentStatus.REMOVED
        );
    }

    @Override
    public boolean existsNotRemovedGateInShiftExcluding(
            UUID shiftId,
            UUID gateId,
            UUID assignmentId
    ) {
        return repository
                .existsByShiftIdAndGateIdAndStatusNotAndShiftAssignmentIdNot(
                        shiftId,
                        gateId,
                        ShiftAssignmentStatus.REMOVED,
                        assignmentId
                );
    }
}