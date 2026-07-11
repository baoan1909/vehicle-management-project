package com.ban.vehicle_management.entrypoint.dto.accesscontrol.lostcardreport.response;

public record LostCardReportSummaryResponse(
        long openCount,
        long unpaidInvoiceCount,
        long resolvedCount,
        long lostCardCount
) {
}
