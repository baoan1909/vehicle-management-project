package com.ban.vehicle_management.entrypoint.dto.iam.partnerregistration.response;

import java.util.UUID;

public record PartnerRegistrationResponse(UUID approvalRequestId, String status, String message) { }
