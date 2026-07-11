package com.ban.vehicle_management.application.accesscontrol.lostcardreport.port.out;

import com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result.LostCardReportListItemResult;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import com.ban.vehicle_management.domain.accesscontrol.lostcardreport.model.LostCardReport;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportContext;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportStatus;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LostCardReportPortOut {

    LostCardReport save(LostCardReport report);

    Optional<LostCardReport> findById(UUID lostCardReportId);

    boolean existsOpenByCardId(UUID cardId);

    boolean existsOpenByParkingSessionId(UUID parkingSessionId);

    List<LostCardReport> findAll(
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

    List<LostCardReportListItemResult> findListItems(
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

    long countByStatus(LostCardReportStatus status);

    long countByStatusAndResolvedAtBetween(LostCardReportStatus status, Instant fromDate, Instant toDate);

    long countOpenByInvoiceStatus(InvoiceStatus invoiceStatus);

    long countDistinctCardsByCardStatus(CardStatus cardStatus);
}
