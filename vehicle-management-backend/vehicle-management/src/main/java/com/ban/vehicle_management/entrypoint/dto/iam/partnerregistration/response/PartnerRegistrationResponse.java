package com.ban.vehicle_management.entrypoint.dto.iam.partnerregistration.response;

import java.util.UUID;

public record PartnerRegistrationResponse(
        UUID accountId,
        UUID approvalRequestId,
        String accountStatus,
        String approvalStatus,
        String nextAction
) { }
