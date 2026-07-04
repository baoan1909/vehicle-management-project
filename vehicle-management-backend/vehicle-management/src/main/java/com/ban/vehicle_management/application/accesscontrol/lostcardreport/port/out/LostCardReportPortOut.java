package com.ban.vehicle_management.application.accesscontrol.lostcardreport.port.out;

import com.ban.vehicle_management.domain.accesscontrol.lostcardreport.model.LostCardReport;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportContext;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LostCardReportPortOut {

    LostCardReport save(LostCardReport report);

    Optional<LostCardReport> findById(UUID lostCardReportId);

    boolean existsOpenByCardId(UUID cardId);

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
}