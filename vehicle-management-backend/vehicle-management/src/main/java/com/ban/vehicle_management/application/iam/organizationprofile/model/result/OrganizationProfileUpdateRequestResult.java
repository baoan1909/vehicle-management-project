package com.ban.vehicle_management.application.iam.organizationprofile.model.result;

import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import java.time.Instant;
import java.util.UUID;

/** A safe projection of one approval request, including only the proposed profile fields. */
public record OrganizationProfileUpdateRequestResult(
        UUID approvalRequestId,
        UUID organizationId,
        String organizationCode,
        String organizationName,
        String addressDetail,
        String provinceCode,
        String districtCode,
        String wardCode,
        String addressDisplay,
        ApprovalRequestStatus status,
        Instant submittedAt,
        String note,
        String currentOrganizationName,
        String currentAddressDisplay
) {
}
