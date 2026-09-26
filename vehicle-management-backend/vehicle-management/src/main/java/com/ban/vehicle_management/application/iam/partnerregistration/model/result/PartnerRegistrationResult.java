package com.ban.vehicle_management.application.iam.partnerregistration.model.result;

import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import java.time.Instant;
import java.util.UUID;

public record PartnerRegistrationResult(
        UUID approvalRequestId,
        String organizationCode,
        String organizationName,
        String representativeName,
        String email,
        String phoneNumber,
        String address,
        Integer expectedParkingLotCount,
        String parkingOperationDescription,
        ApprovalRequestStatus status,
        String note,
        Instant createdAt
) {
}
