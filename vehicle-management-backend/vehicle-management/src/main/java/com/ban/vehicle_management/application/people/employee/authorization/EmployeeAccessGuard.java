package com.ban.vehicle_management.application.people.employee.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.InternalEmployeeApprovalPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class EmployeeAccessGuard {

    private final CurrentAccountPortIn currentAccountPortIn;
    private final InternalEmployeeApprovalPortOut internalEmployeeApprovalPortOut;

    public EmployeeAccessGuard(
            CurrentAccountPortIn currentAccountPortIn,
            InternalEmployeeApprovalPortOut internalEmployeeApprovalPortOut
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.internalEmployeeApprovalPortOut = internalEmployeeApprovalPortOut;
    }

    public void ensureCanRead(Employee employee) {
        ensureParkingManagerTargetsEmployeeOnly(employee);
    }

    public void ensureCanManage(Employee employee) {
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        String targetRole = resolveTargetRole(employee);
        if (isSystemAdmin(currentAccount) && isSystemAdminManagedTarget(targetRole)) {
            return;
        }
        if (isParkingManager(currentAccount) && AdminProvisionableAccountRoleCode.EMPLOYEE.name().equals(targetRole)) {
            return;
        }
        throw new AccessDeniedException("Access is denied");
    }

    public List<Employee> filterReadableEmployees(List<Employee> employees) {
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        if (!isParkingManager(currentAccount)) {
            return employees;
        }
        return employees.stream()
                .filter(this::isEmployeeTarget)
                .toList();
    }

    private void ensureParkingManagerTargetsEmployeeOnly(Employee employee) {
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        if (isParkingManager(currentAccount) && !isEmployeeTarget(employee)) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    private boolean isParkingManager(CurrentAccountAccess currentAccount) {
        return AdminProvisionableAccountRoleCode.PARKING_MANAGER.name().equals(currentAccount.roleCode());
    }

    private boolean isSystemAdmin(CurrentAccountAccess currentAccount) {
        return AdminProvisionableAccountRoleCode.SYSTEM_ADMIN.name().equals(currentAccount.roleCode());
    }

    private boolean isSystemAdminManagedTarget(String roleCode) {
        return AdminProvisionableAccountRoleCode.SYSTEM_ADMIN.name().equals(roleCode)
                || AdminProvisionableAccountRoleCode.PARKING_MANAGER.name().equals(roleCode);
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
