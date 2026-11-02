package com.ban.vehicle_management.application.iam.organization.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.AccountAccessInvalidationPortIn;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.iam.organization.authorization.ParkingLotScopeResolver;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.application.iam.organization.port.out.ParkingLotAssignmentPortOut;
import com.ban.vehicle_management.application.people.employee.port.out.EmployeePortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.iam.organization.model.ParkingLotManagementAssignment;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.domain.people.employee.model.EmployeeParkingLotAssignment;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.iam.ManagementAssignmentRole;
import com.ban.vehicle_management.shared.enumeration.iam.ManagementAssignmentStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotManagementState;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeParkingLotAssignmentSource;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeParkingLotAssignmentType;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParkingLotAssignmentUseCaseImplTest {
    @Mock private CurrentAccountPortIn currentAccountPortIn;
    @Mock private OrganizationAccessGuard organizationAccessGuard;
    @Mock private OrganizationPortOut organizationPortOut;
    @Mock private ParkingLotAssignmentPortOut assignmentPortOut;
    @Mock private ParkingLotScopeResolver scopeResolver;
    @Mock private EmployeePortOut employeePortOut;
    @Mock private AccountAccessInvalidationPortIn invalidationPortIn;
    @Mock private AuditLogPortOut auditLogPortOut;
    @InjectMocks private ParkingLotAssignmentUseCaseImpl useCase;

    private UUID actorId;

    @BeforeEach
    void setUp() {
        actorId = UUID.randomUUID();
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(actorId);
        org.mockito.Mockito.lenient().when(assignmentPortOut.saveManagementAssignment(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.lenient().when(assignmentPortOut.saveManagementAssignments(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void emergencyManagerRevocationKeepsEmployeeAssignmentsAndMovesLotToPartnerSupervision() {
        UUID managerId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        ParkingLotManagementAssignment assignment = managementAssignment(managerId, lotId);
        when(assignmentPortOut.findManagerAssignmentsForUpdate(eq(managerId), any()))
                .thenReturn(List.of(assignment));

        useCase.revokeManagerImmediately(managerId, "Security incident");

        assertEquals(ManagementAssignmentStatus.ENDED, assignment.getStatus());
        assertEquals(actorId, assignment.getEndedBy());
        verify(assignmentPortOut).updateParkingLotManagementState(
                lotId, ParkingLotManagementState.PARTNER_SUPERVISED);
        verify(invalidationPortIn).invalidateAccount(
                managerId, "Quyền quản lý bãi xe đã bị thu hồi.", "parking", "parking_lots", lotId);
        verifyNoInteractions(employeePortOut);
    }

    @Test
    void managerReplacementChangesOnlyManagementAssignments() {
        UUID organizationId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        UUID outgoingId = UUID.randomUUID();
        UUID incomingId = UUID.randomUUID();
        ParkingLotManagementAssignment outgoing = managementAssignment(outgoingId, lotId);
        when(organizationPortOut.allParkingLotsBelongToOrganization(organizationId, Set.of(lotId))).thenReturn(true);
        when(organizationPortOut.isActiveAccountWithRole(incomingId, OrganizationAccessGuard.PARKING_MANAGER)).thenReturn(true);
        when(organizationPortOut.findActiveMembershipId(organizationId, incomingId)).thenReturn(Optional.of(UUID.randomUUID()));
        when(assignmentPortOut.findManagementAssignmentsForUpdate(eq(lotId), any(), any())).thenReturn(List.of(outgoing));

        ParkingLotManagementAssignment incoming = useCase.replaceManager(
                organizationId, lotId, outgoingId, incomingId, Instant.now(), false, "Normal handover");

        assertEquals(ManagementAssignmentStatus.ENDED, outgoing.getStatus());
        assertEquals(ManagementAssignmentStatus.ACTIVE, incoming.getStatus());
        assertEquals(incomingId, incoming.getManagerAccountId());
        verify(assignmentPortOut).updateParkingLotManagementState(lotId, ParkingLotManagementState.MANAGED);
        verifyNoInteractions(employeePortOut);
    }

    @Test
    void scheduledManagerReplacementKeepsOutgoingScopeUntilEffectiveTime() {
        UUID organizationId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        UUID outgoingId = UUID.randomUUID();
        UUID incomingId = UUID.randomUUID();
        Instant effectiveAt = Instant.now().plusSeconds(3600);
        ParkingLotManagementAssignment outgoing = managementAssignment(outgoingId, lotId);
        when(organizationPortOut.allParkingLotsBelongToOrganization(organizationId, Set.of(lotId))).thenReturn(true);
        when(organizationPortOut.isActiveAccountWithRole(incomingId, OrganizationAccessGuard.PARKING_MANAGER)).thenReturn(true);
        when(organizationPortOut.findActiveMembershipId(organizationId, incomingId)).thenReturn(Optional.of(UUID.randomUUID()));
        when(assignmentPortOut.findManagementAssignmentsForUpdate(eq(lotId), eq(effectiveAt), any()))
                .thenReturn(List.of(outgoing));

        useCase.replaceManager(
                organizationId, lotId, outgoingId, incomingId, effectiveAt, true, "Scheduled handover");

        assertEquals(ManagementAssignmentStatus.ACTIVE, outgoing.getStatus());
        assertEquals(effectiveAt, outgoing.getEffectiveTo());
        verify(assignmentPortOut).updateParkingLotManagementState(lotId, ParkingLotManagementState.HANDOVER);
    }

    @Test
    void emergencyRevocationKeepsLotManagedWhenAnotherManagerIsStillEffective() {
        UUID managerId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        ParkingLotManagementAssignment assignment = managementAssignment(managerId, lotId);
        when(assignmentPortOut.findManagerAssignmentsForUpdate(eq(managerId), any()))
                .thenReturn(List.of(assignment));
        when(assignmentPortOut.hasEffectiveActiveManager(eq(lotId), any())).thenReturn(true);

        useCase.revokeManagerImmediately(managerId, "Security incident");

        verify(assignmentPortOut).updateParkingLotManagementState(lotId, ParkingLotManagementState.MANAGED);
    }

    @Test
    void managerMayAssignOnlyAnEmployeeOwnedByTheLotOrganization() {
        UUID organizationId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID employeeAccountId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        actorId = managerId;
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(managerId);
        CurrentAccountAccess manager = account(managerId, "PARKING_MANAGER",
                Set.of("EMPLOYEE_UPDATE_ALL", OrganizationAccessGuard.PARKING_SCOPE_ASSIGNED));
        Employee employee = new Employee();
        employee.setEmployeeId(employeeId);
        employee.setStatus(EmployeeStatus.ACTIVE);
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(manager);
        when(organizationPortOut.allParkingLotsBelongToOrganization(organizationId, Set.of(lotId))).thenReturn(true);
        when(scopeResolver.resolveManagerManagedLots(managerId)).thenReturn(Set.of(lotId));
        when(employeePortOut.findById(employeeId)).thenReturn(Optional.of(employee));
        when(employeePortOut.findAccountIdByEmployeeId(employeeId)).thenReturn(Optional.of(employeeAccountId));
        when(employeePortOut.hasAccountRole(employeeId, "EMPLOYEE")).thenReturn(true);
        when(organizationPortOut.findActiveMembershipId(organizationId, employeeAccountId))
                .thenReturn(Optional.of(UUID.randomUUID()));
        when(assignmentPortOut.findEmployeeAssignmentsForUpdate(eq(employeeId), eq(lotId), any())).thenReturn(List.of());
        when(assignmentPortOut.saveEmployeeAssignment(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EmployeeParkingLotAssignment saved = useCase.assignEmployee(
                organizationId, lotId, employeeId, EmployeeParkingLotAssignmentType.PRIMARY,
                Instant.now(), null);

        assertEquals(EmployeeParkingLotAssignmentSource.MANAGER, saved.getAssignmentSource());
        assertEquals(employeeId, saved.getEmployeeId());
        verify(invalidationPortIn).invalidateAccount(
                employeeAccountId, "Phạm vi bãi xe làm việc đã thay đổi.", "parking", "parking_lots", lotId);
    }

    private ParkingLotManagementAssignment managementAssignment(UUID managerId, UUID lotId) {
        ParkingLotManagementAssignment assignment = new ParkingLotManagementAssignment();
        assignment.setAssignmentId(UUID.randomUUID());
        assignment.setOrganizationId(UUID.randomUUID());
        assignment.setParkingLotId(lotId);
        assignment.setManagerAccountId(managerId);
        assignment.setManagementRole(ManagementAssignmentRole.PRIMARY);
        assignment.setStatus(ManagementAssignmentStatus.ACTIVE);
        assignment.setEffectiveFrom(Instant.now().minusSeconds(60));
        assignment.setAssignedBy(actorId);
        return assignment;
    }

    private CurrentAccountAccess account(UUID accountId, String roleCode, Set<String> permissions) {
        return new CurrentAccountAccess(
                accountId, "subject", "manager", "manager@example.com", UUID.randomUUID(), roleCode,
                true, true, Set.of(UUID.randomUUID()), Set.of(UUID.randomUUID()), AccountStatus.ACTIVE,
                EmployeeStatus.ACTIVE, null, null, false, permissions);
    }
}
