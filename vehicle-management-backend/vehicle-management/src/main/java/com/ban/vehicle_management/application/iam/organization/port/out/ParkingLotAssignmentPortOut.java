package com.ban.vehicle_management.application.iam.organization.port.out;

import com.ban.vehicle_management.domain.iam.organization.model.ParkingLotManagementAssignment;
import com.ban.vehicle_management.domain.people.employee.model.EmployeeParkingLotAssignment;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotManagementState;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ParkingLotAssignmentPortOut {
    Set<UUID> findManagerManagedLotIds(UUID accountId, Instant at);
    Set<UUID> findEmployeeReadableLotIds(UUID accountId, Instant at);
    Set<UUID> findEmployeeOperableLotIds(UUID accountId, Instant at);
    Set<UUID> findAssignedEmployeeIds(UUID parkingLotId, Instant at);
    Optional<EmployeeParkingLotAssignment> findEmployeeAssignmentCovering(
            UUID employeeId, UUID parkingLotId, Instant fromTime, Instant toTime);
    List<ParkingLotManagementAssignment> findManagementAssignmentsForUpdate(
            UUID parkingLotId, Instant periodStart, Instant periodEnd);
    List<ParkingLotManagementAssignment> findManagerAssignmentsForUpdate(UUID managerAccountId, Instant at);
    Optional<ParkingLotManagementAssignment> findManagementAssignmentForUpdate(UUID assignmentId);
    List<ParkingLotManagementAssignment> findManagementHistory(UUID parkingLotId);
    boolean hasEffectiveActiveManager(UUID parkingLotId, Instant at);
    ParkingLotManagementAssignment saveManagementAssignment(ParkingLotManagementAssignment assignment);
    List<ParkingLotManagementAssignment> saveManagementAssignments(List<ParkingLotManagementAssignment> assignments);
    EmployeeParkingLotAssignment saveEmployeeAssignment(EmployeeParkingLotAssignment assignment);
    List<EmployeeParkingLotAssignment> findEmployeeAssignmentsForUpdate(
            UUID employeeId, UUID parkingLotId, Instant at);
    void lockParkingLot(UUID parkingLotId);
    void updateParkingLotManagementState(UUID parkingLotId, ParkingLotManagementState state);
    ParkingLotManagementState findParkingLotManagementState(UUID parkingLotId);
}
