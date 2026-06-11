package com.ban.vehicle_management.application.operations.approvalrequest.model.result;

import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import java.util.UUID;

public record CustomerOnboardingApprovalCandidate(
        UUID accountId,
        UUID userProfileId,
        UUID customerId,
        String roleCode,
        AccountStatus accountStatus,
        CustomerStatus customerStatus,
        CustomerApprovalStatus customerApprovalStatus
) {
}
