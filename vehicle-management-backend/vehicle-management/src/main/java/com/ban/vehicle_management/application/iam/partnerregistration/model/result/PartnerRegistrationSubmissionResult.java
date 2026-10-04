package com.ban.vehicle_management.application.iam.partnerregistration.model.result;

import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import java.util.UUID;

public record PartnerRegistrationSubmissionResult(
        UUID accountId,
        UUID approvalRequestId,
        AccountStatus accountStatus,
        ApprovalRequestStatus approvalStatus,
        String nextAction
) {
}
