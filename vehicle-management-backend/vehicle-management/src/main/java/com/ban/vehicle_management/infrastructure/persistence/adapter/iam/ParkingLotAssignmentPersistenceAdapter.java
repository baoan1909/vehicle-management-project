package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import com.ban.vehicle_management.application.iam.organization.port.out.ParkingLotAssignmentPortOut;
import com.ban.vehicle_management.domain.iam.organization.model.ParkingLotManagementAssignment;
import com.ban.vehicle_management.domain.people.employee.model.EmployeeParkingLotAssignment;
import com.ban.vehicle_management.infrastructure.mapper.iam.ParkingLotManagementAssignmentPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.people.EmployeeParkingLotAssignmentPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.ParkingLotManagementAssignmentRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingLotRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.EmployeeParkingLotAssignmentRepository;
import com.ban.vehicle_management.shared.enumeration.iam.ManagementAssignmentStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotManagementState;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeParkingLotAssignmentStatus;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ParkingLotAssignmentPersistenceAdapter implements ParkingLotAssignmentPortOut {
    private static final Set<ManagementAssignmentStatus> LIVE_MANAGEMENT_STATUSES =
            Set.of(ManagementAssignmentStatus.ACTIVE, ManagementAssignmentStatus.PENDING_ACCEPTANCE);
    private static final Set<EmployeeParkingLotAssignmentStatus> LIVE_EMPLOYEE_STATUSES =
            Set.of(EmployeeParkingLotAssignmentStatus.ACTIVE, EmployeeParkingLotAssignmentStatus.SCHEDULED);

    private final ParkingLotManagementAssignmentRepository managementRepository;
    private final EmployeeParkingLotAssignmentRepository employeeRepository;
    private final ParkingLotRepository parkingLotRepository;
    private final ParkingLotManagementAssignmentPersistenceMapper managementMapper;
    private final EmployeeParkingLotAssignmentPersistenceMapper employeeMapper;

    public ParkingLotAssignmentPersistenceAdapter(
            ParkingLotManagementAssignmentRepository managementRepository,
            EmployeeParkingLotAssignmentRepository employeeRepository,
            ParkingLotRepository parkingLotRepository,
            ParkingLotManagementAssignmentPersistenceMapper managementMapper,
            EmployeeParkingLotAssignmentPersistenceMapper employeeMapper
    ) {
        this.managementRepository = managementRepository;
        this.employeeRepository = employeeRepository;
        this.parkingLotRepository = parkingLotRepository;
        this.managementMapper = managementMapper;
        this.employeeMapper = employeeMapper;
    }

    @Override
    public Set<UUID> findManagerManagedLotIds(UUID accountId, Instant at) {
        return managementRepository.findEffectiveParkingLotIds(
                accountId, ManagementAssignmentStatus.ACTIVE, at);
    }

    @Override
    public Set<UUID> findEmployeeReadableLotIds(UUID accountId, Instant at) {
        return employeeRepository.findReadableParkingLotIds(accountId, at);
    }

    @Override
    public Set<UUID> findEmployeeOperableLotIds(UUID accountId, Instant at) {
        return employeeRepository.findOperableParkingLotIds(accountId, at);
    }

    @Override
    public Set<UUID> findAssignedEmployeeIds(UUID parkingLotId, Instant at) {
        return employeeRepository.findAssignedEmployeeIds(parkingLotId, LIVE_EMPLOYEE_STATUSES, at);
    }

    @Override
    public Optional<EmployeeParkingLotAssignment> findEmployeeAssignmentCovering(
            UUID employeeId, UUID parkingLotId, Instant fromTime, Instant toTime) {
        return employeeRepository.findCovering(
                employeeId, parkingLotId, LIVE_EMPLOYEE_STATUSES, fromTime, toTime).map(employeeMapper::toDomain);
    }

    @Override
    public List<ParkingLotManagementAssignment> findManagementAssignmentsForUpdate(
            UUID parkingLotId, Instant periodStart, Instant periodEnd) {
        return managementRepository.findOverlappingForUpdate(
                parkingLotId, LIVE_MANAGEMENT_STATUSES, periodStart, periodEnd).stream()
                .map(managementMapper::toDomain).toList();
    }

    @Override
    public List<ParkingLotManagementAssignment> findManagerAssignmentsForUpdate(UUID managerAccountId, Instant at) {
        return managementRepository.findEffectiveByManagerForUpdate(
                managerAccountId, LIVE_MANAGEMENT_STATUSES, at).stream().map(managementMapper::toDomain).toList();
    }

    @Override
    public Optional<ParkingLotManagementAssignment> findManagementAssignmentForUpdate(UUID assignmentId) {
        return managementRepository.findByIdForUpdate(assignmentId).map(managementMapper::toDomain);
    }

    @Override
    public List<ParkingLotManagementAssignment> findManagementHistory(UUID parkingLotId) {
        return managementRepository.findAllByParkingLotIdOrderByEffectiveFromDesc(parkingLotId).stream()
                .map(managementMapper::toDomain).toList();
    }

    @Override
    public boolean hasEffectiveActiveManager(UUID parkingLotId, Instant at) {
        return managementRepository.hasEffectiveActiveManager(parkingLotId, at);
    }

    @Override
    public ParkingLotManagementAssignment saveManagementAssignment(ParkingLotManagementAssignment assignment) {
        return managementMapper.toDomain(managementRepository.saveAndFlush(managementMapper.toEntity(assignment)));
    }

    @Override
    public List<ParkingLotManagementAssignment> saveManagementAssignments(
            List<ParkingLotManagementAssignment> assignments) {
        return managementRepository.saveAllAndFlush(assignments.stream().map(managementMapper::toEntity).toList())
                .stream().map(managementMapper::toDomain).toList();
    }

    @Override
    public EmployeeParkingLotAssignment saveEmployeeAssignment(EmployeeParkingLotAssignment assignment) {
        return employeeMapper.toDomain(employeeRepository.saveAndFlush(employeeMapper.toEntity(assignment)));
    }

    @Override
    public List<EmployeeParkingLotAssignment> findEmployeeAssignmentsForUpdate(
            UUID employeeId, UUID parkingLotId, Instant at) {
        return employeeRepository.findEffectiveForUpdate(employeeId, parkingLotId, LIVE_EMPLOYEE_STATUSES, at)
                .stream().map(employeeMapper::toDomain).toList();
    }

    @Override
    public void lockParkingLot(UUID parkingLotId) {
        parkingLotRepository.findByIdForUpdate(parkingLotId)
                .orElseThrow(() -> new NotFoundException("Parking lot not found"));
    }

    @Override
    public void updateParkingLotManagementState(UUID parkingLotId, ParkingLotManagementState state) {
        parkingLotRepository.updateManagementState(parkingLotId, state);
    }

    @Override
    public ParkingLotManagementState findParkingLotManagementState(UUID parkingLotId) {
        return parkingLotRepository.findById(parkingLotId)
                .map(entity -> entity.getManagementState())
                .orElseThrow(() -> new NotFoundException("Parking lot not found"));
    }
}
