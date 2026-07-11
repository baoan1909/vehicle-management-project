package com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result;

public record LostCardReportSummaryResult(
        long openCount,
        long unpaidInvoiceCount,
        long resolvedCount,
        long lostCardCount
) {
}
