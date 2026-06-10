package com.ban.vehicle_management.application.operations.approvalrequest.model.result;

import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import java.util.UUID;

public record InternalEmployeeApprovalCandidate(
        UUID accountId,
        UUID userProfileId,
        UUID employeeId,
        String roleCode,
        AccountStatus accountStatus,
        EmployeeStatus employeeStatus
) {
}
