package com.ban.vehicle_management.application.iam.organization.port.in;

import com.ban.vehicle_management.domain.iam.organization.model.ParkingLotManagementAssignment;
import com.ban.vehicle_management.domain.people.employee.model.EmployeeParkingLotAssignment;
import com.ban.vehicle_management.shared.enumeration.iam.ManagementAssignmentRole;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeParkingLotAssignmentType;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ParkingLotAssignmentPortIn {
    ParkingLotManagementAssignment assignManager(
            UUID organizationId, UUID parkingLotId, UUID managerAccountId,
            ManagementAssignmentRole role, Instant effectiveAt, boolean acceptanceRequired, String handoverNote);
    ParkingLotManagementAssignment replaceManager(
            UUID organizationId, UUID parkingLotId, UUID outgoingManagerId,
            UUID incomingManagerId, Instant effectiveAt, boolean acceptanceRequired, String handoverNote);
    ParkingLotManagementAssignment acceptManagementAssignment(UUID assignmentId);
    void revokeManagerImmediately(UUID managerAccountId, String reason);
    void endManagerAssignment(UUID organizationId, UUID parkingLotId, UUID managerAccountId, String reason);
    List<ParkingLotManagementAssignment> getManagementHistory(UUID organizationId, UUID parkingLotId);
    EmployeeParkingLotAssignment assignEmployee(
            UUID organizationId, UUID parkingLotId, UUID employeeId,
            EmployeeParkingLotAssignmentType type, Instant effectiveFrom, Instant effectiveTo);
    List<EmployeeParkingLotAssignment> getEmployeeAssignments(UUID organizationId);
    EmployeeParkingLotAssignment transferEmployee(
            UUID organizationId, UUID employeeId, UUID sourceParkingLotId,
            UUID targetParkingLotId, Instant effectiveAt, String reason);
    Set<UUID> getMyReadableParkingLots();
    Set<UUID> getMyOperableParkingLots();
    void suspendParkingLotOperations(UUID organizationId, UUID parkingLotId, String reason);
    void resumeParkingLotOperations(UUID organizationId, UUID parkingLotId);
}
