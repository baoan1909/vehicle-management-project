package com.ban.vehicle_management.application.people.employee.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.InternalEmployeeApprovalPortOut;
import com.ban.vehicle_management.application.people.employee.port.out.EmployeePortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class EmployeeAccessGuard {

    private final CurrentAccountPortIn currentAccountPortIn;
    private final InternalEmployeeApprovalPortOut internalEmployeeApprovalPortOut;
    private final EmployeePortOut employeePortOut;
    private final OrganizationPortOut organizationPortOut;

    public EmployeeAccessGuard(
            CurrentAccountPortIn currentAccountPortIn,
            InternalEmployeeApprovalPortOut internalEmployeeApprovalPortOut,
            EmployeePortOut employeePortOut,
            OrganizationPortOut organizationPortOut
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.internalEmployeeApprovalPortOut = internalEmployeeApprovalPortOut;
        this.employeePortOut = employeePortOut;
        this.organizationPortOut = organizationPortOut;
    }

    public void ensureCanRead(Employee employee) {
        if (!canRead(currentAccountPortIn.getCurrentAccountOrThrow(), employee)) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    public void ensureCanManage(Employee employee) {
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        String targetRole = resolveTargetRole(employee);
        if (isPartnerAdmin(currentAccount)
                && isPartnerManagedTarget(targetRole)
                && sharesActiveOrganization(currentAccount, employee)) {
            return;
        }
        if (isParkingManager(currentAccount)
                && AdminProvisionableAccountRoleCode.EMPLOYEE.name().equals(targetRole)
                && hasParkingLotAssignment(currentAccount)
                && sharesActiveOrganization(currentAccount, employee)) {
            return;
        }
        throw new AccessDeniedException("Access is denied");
    }

    public List<Employee> filterReadableEmployees(List<Employee> employees) {
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        return employees.stream()
                .filter(employee -> canRead(currentAccount, employee))
                .toList();
    }

    private boolean canRead(CurrentAccountAccess currentAccount, Employee employee) {
        if (isSystemAdmin(currentAccount)) {
            return true;
        }
        if (isPartnerAdmin(currentAccount)) {
            return isPartnerManagedTarget(resolveTargetRole(employee))
                    && sharesActiveOrganization(currentAccount, employee);
        }
        return isParkingManager(currentAccount)
                && hasParkingLotAssignment(currentAccount)
                && isEmployeeTarget(employee)
                && sharesActiveOrganization(currentAccount, employee);
    }

    private boolean isParkingManager(CurrentAccountAccess currentAccount) {
        return AdminProvisionableAccountRoleCode.PARKING_MANAGER.name().equals(currentAccount.roleCode());
    }

    private boolean isSystemAdmin(CurrentAccountAccess currentAccount) {
        return AdminProvisionableAccountRoleCode.SYSTEM_ADMIN.name().equals(currentAccount.roleCode());
    }

    private boolean isPartnerAdmin(CurrentAccountAccess currentAccount) {
        return AdminProvisionableAccountRoleCode.PARTNER_ADMIN.name().equals(currentAccount.roleCode());
    }

    private boolean isPartnerManagedTarget(String roleCode) {
        return AdminProvisionableAccountRoleCode.PARKING_MANAGER.name().equals(roleCode)
                || AdminProvisionableAccountRoleCode.EMPLOYEE.name().equals(roleCode);
    }

    private boolean hasParkingLotAssignment(CurrentAccountAccess currentAccount) {
        return !organizationPortOut.findScopedParkingLotIdsByAccountId(currentAccount.accountId()).isEmpty();
    }

    private boolean sharesActiveOrganization(CurrentAccountAccess currentAccount, Employee employee) {
        Set<UUID> actorOrganizations = organizationPortOut
                .findActiveOrganizationIdsByAccountId(currentAccount.accountId());
        if (actorOrganizations.isEmpty() || employee == null || employee.getEmployeeId() == null) {
            return false;
        }
        return employeePortOut.findAccountIdByEmployeeId(employee.getEmployeeId())
                .map(organizationPortOut::findActiveOrganizationIdsByAccountId)
                .stream()
                .flatMap(Set::stream)
                .anyMatch(actorOrganizations::contains);
    }

    private boolean isEmployeeTarget(Employee employee) {
        return AdminProvisionableAccountRoleCode.EMPLOYEE.name().equals(resolveTargetRole(employee));
    }

    private String resolveTargetRole(Employee employee) {
        if (employee == null || employee.getEmployeeId() == null) {
            return null;
        }
        if (employee.getRoleCode() != null && !employee.getRoleCode().isBlank()) {
            return employee.getRoleCode();
        }
        return internalEmployeeApprovalPortOut.findCandidateByEmployeeId(employee.getEmployeeId())
                .map(candidate -> candidate.roleCode())
                .orElse(null);
    }
}
