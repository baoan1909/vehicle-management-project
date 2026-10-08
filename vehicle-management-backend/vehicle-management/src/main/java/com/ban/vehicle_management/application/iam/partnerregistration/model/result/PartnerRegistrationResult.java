package com.ban.vehicle_management.application.iam.partnerregistration.model.result;

import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import java.time.Instant;
import java.util.UUID;

public record PartnerRegistrationResult(
        UUID approvalRequestId,
        String organizationCode,
        String organizationName,
        String organizationAddressDisplay,
        String applicantFullName,
        String applicantPhoneNumber,
        String applicantEmail,
        Instant submittedAt,
        ApprovalRequestStatus status,
        String note
) {
}
