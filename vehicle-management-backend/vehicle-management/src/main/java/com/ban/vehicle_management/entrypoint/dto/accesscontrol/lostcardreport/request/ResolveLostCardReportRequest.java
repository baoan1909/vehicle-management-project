package com.ban.vehicle_management.entrypoint.dto.accesscontrol.lostcardreport.request;

import java.util.UUID;

public record ResolveLostCardReportRequest(
        UUID newCardId
) {
}