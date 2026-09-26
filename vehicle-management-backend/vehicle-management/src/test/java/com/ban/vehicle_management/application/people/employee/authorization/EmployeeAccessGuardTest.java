package com.ban.vehicle_management.application.people.employee.authorization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.InternalEmployeeApprovalPortOut;
import com.ban.vehicle_management.application.people.employee.port.out.EmployeePortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class EmployeeAccessGuardTest {

    @Mock private CurrentAccountPortIn currentAccountPortIn;
    @Mock private InternalEmployeeApprovalPortOut internalEmployeeApprovalPortOut;
    @Mock private EmployeePortOut employeePortOut;
    @Mock private OrganizationPortOut organizationPortOut;
    @InjectMocks private EmployeeAccessGuard employeeAccessGuard;

    @Test
    void partnerCanManageOwnManagerAndEmployee() {
        CurrentAccountAccess partner = account("PARTNER_ADMIN");
        Employee manager = employee("PARKING_MANAGER");
        Employee staff = employee("EMPLOYEE");
        UUID organizationId = UUID.randomUUID();
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(partner);
        ownOrganization(partner, manager, organizationId);
        employeeAccessGuard.ensureCanManage(manager);
        ownOrganization(partner, staff, organizationId);
        employeeAccessGuard.ensureCanManage(staff);
        assertEquals(2, employeeAccessGuard.filterReadableEmployees(List.of(manager, staff)).size());
    }

    @Test
    void partnerCannotReadOrManageAnotherPartnerEmployee() {
        CurrentAccountAccess partner = account("PARTNER_ADMIN");
        Employee staff = employee("EMPLOYEE");
        UUID staffAccountId = UUID.randomUUID();
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(partner);
        when(organizationPortOut.findActiveOrganizationIdsByAccountId(partner.accountId()))
                .thenReturn(Set.of(UUID.randomUUID()));
        when(employeePortOut.findAccountIdByEmployeeId(staff.getEmployeeId()))
                .thenReturn(Optional.of(staffAccountId));
        when(organizationPortOut.findActiveOrganizationIdsByAccountId(staffAccountId))
                .thenReturn(Set.of(UUID.randomUUID()));

        assertThrows(AccessDeniedException.class, () -> employeeAccessGuard.ensureCanRead(staff));
        assertThrows(AccessDeniedException.class, () -> employeeAccessGuard.ensureCanManage(staff));
        assertEquals(List.of(), employeeAccessGuard.filterReadableEmployees(List.of(staff)));
    }

    @Test
    void managerRequiresLotAssignmentAndPartnerOwnership() {
        CurrentAccountAccess manager = account("PARKING_MANAGER");
        Employee staff = employee("EMPLOYEE");
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(manager);
        when(organizationPortOut.findScopedParkingLotIdsByAccountId(manager.accountId()))
                .thenReturn(Set.of(), Set.of(UUID.randomUUID()));

        assertThrows(AccessDeniedException.class, () -> employeeAccessGuard.ensureCanManage(staff));
        ownOrganization(manager, staff, UUID.randomUUID());
        employeeAccessGuard.ensureCanManage(staff);
    }

    @Test
    void managerCannotReadOrManageAnotherManager() {
        CurrentAccountAccess manager = account("PARKING_MANAGER");
        Employee targetManager = employee("PARKING_MANAGER");
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(manager);
        when(organizationPortOut.findScopedParkingLotIdsByAccountId(manager.accountId()))
                .thenReturn(Set.of(UUID.randomUUID()));

        assertThrows(AccessDeniedException.class, () -> employeeAccessGuard.ensureCanRead(targetManager));
        assertThrows(AccessDeniedException.class, () -> employeeAccessGuard.ensureCanManage(targetManager));
    }

    @Test
    void systemAdminCanReadAllButCannotChangePartnerStaff() {
        CurrentAccountAccess systemAdmin = account("SYSTEM_ADMIN");
        Employee staff = employee("EMPLOYEE");
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(systemAdmin);

        employeeAccessGuard.ensureCanRead(staff);
        assertEquals(1, employeeAccessGuard.filterReadableEmployees(List.of(staff)).size());
        assertThrows(AccessDeniedException.class, () -> employeeAccessGuard.ensureCanManage(staff));
    }

    private void ownOrganization(CurrentAccountAccess actor, Employee target, UUID organizationId) {
        UUID targetAccountId = UUID.randomUUID();
        when(organizationPortOut.findActiveOrganizationIdsByAccountId(actor.accountId()))
                .thenReturn(Set.of(organizationId));
        when(employeePortOut.findAccountIdByEmployeeId(target.getEmployeeId()))
                .thenReturn(Optional.of(targetAccountId));
        when(organizationPortOut.findActiveOrganizationIdsByAccountId(targetAccountId))
                .thenReturn(Set.of(organizationId));
    }

    private Employee employee(String roleCode) {
        Employee employee = new Employee();
        employee.setEmployeeId(UUID.randomUUID());
        employee.setRoleCode(roleCode);
        employee.setStatus(EmployeeStatus.ACTIVE);
        return employee;
    }

    private CurrentAccountAccess account(String roleCode) {
        return new CurrentAccountAccess(
                UUID.randomUUID(), "subject", "username", "user@example.com",
                UUID.randomUUID(), roleCode, AccountStatus.ACTIVE, EmployeeStatus.ACTIVE,
                Set.of("EMPLOYEE_READ_ALL", "EMPLOYEE_UPDATE_ALL", "EMPLOYEE_DELETE_ALL")
        );
    }
}
