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
        ensureParkingManagerTargetsEmployeeOnly(employee);
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

    private boolean isEmployeeTarget(Employee employee) {
        if (employee == null || employee.getEmployeeId() == null) {
            return false;
        }
        return internalEmployeeApprovalPortOut.findCandidateByEmployeeId(employee.getEmployeeId())
                .map(candidate -> AdminProvisionableAccountRoleCode.EMPLOYEE.name().equals(candidate.roleCode()))
                .orElse(false);
    }
}
