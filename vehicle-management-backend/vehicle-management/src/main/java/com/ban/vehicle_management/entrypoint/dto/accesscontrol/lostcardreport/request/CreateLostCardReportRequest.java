package com.ban.vehicle_management.entrypoint.dto.accesscontrol.lostcardreport.request;

import java.time.Instant;
import java.util.UUID;

public record CreateLostCardReportRequest(
        UUID parkingSessionId,
        UUID subscriptionId,
        Instant timeOfLost,
        String reporterName,
        String reporterPhone,
        String identifyCard,
        String registrationLicense,
        String note
) {
}