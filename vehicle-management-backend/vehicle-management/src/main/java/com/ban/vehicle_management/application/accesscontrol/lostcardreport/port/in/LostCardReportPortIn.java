package com.ban.vehicle_management.application.accesscontrol.lostcardreport.port.in;

import com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result.LostCardPreviewResult;
import com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result.LostCardReportDetailResult;
import com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result.LostCardReportListItemResult;
import com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result.LostCardReportSummaryResult;
import com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result.LostCardReportWorkflowResult;
import com.ban.vehicle_management.domain.accesscontrol.lostcardreport.model.LostCardReport;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportContext;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface LostCardReportPortIn {

    LostCardPreviewResult previewByLicensePlate(String licensePlate);

    LostCardReportWorkflowResult createReport(LostCardReport report);

    LostCardReportWorkflowResult resolveReport(UUID lostCardReportId, UUID newCardId);

    LostCardReportWorkflowResult cancelReport(UUID lostCardReportId, String cancelReason);

    LostCardReportDetailResult getReportById(UUID lostCardReportId);

    List<LostCardReport> getReports(
            LostCardReportStatus status,
            LostCardReportContext context,
            UUID customerId,
            UUID cardId,
            UUID parkingSessionId,
            UUID subscriptionId,
            Instant fromDate,
            Instant toDate,
            String keyword
    );

    List<LostCardReportListItemResult> getReportListItems(
            LostCardReportStatus status,
            LostCardReportContext context,
            UUID customerId,
            UUID cardId,
            UUID parkingSessionId,
            UUID subscriptionId,
            Instant fromDate,
            Instant toDate,
            String keyword
    );

    LostCardReportSummaryResult getSummary(Instant fromDate, Instant toDate);
}
