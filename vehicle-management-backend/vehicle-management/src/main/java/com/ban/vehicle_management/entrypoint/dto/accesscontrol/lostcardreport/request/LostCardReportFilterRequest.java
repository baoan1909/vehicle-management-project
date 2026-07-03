package com.ban.vehicle_management.entrypoint.dto.accesscontrol.lostcardreport.request;

import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportContext;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportStatus;
import java.time.Instant;
import java.util.UUID;

public record LostCardReportFilterRequest(
        LostCardReportStatus status,
        LostCardReportContext context,
        UUID customerId,
        UUID cardId,
        UUID parkingSessionId,
        UUID subscriptionId,
        Instant fromDate,
        Instant toDate,
        String keyword
) {
}